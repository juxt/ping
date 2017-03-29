;; Copyright © 2016, JUXT LTD.
(require '[clojure.java.shell :as sh])

(def repl-port 5600)
(def project "ping")
(def version "0.0.1-SNAPSHOT" ;(deduce-version-from-git)
  )

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :asset-paths #{"assets"}
 :dependencies
 '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [reloaded.repl "0.2.3" :scope "test"]
   [weasel "0.7.0" :scope "test"]

   [org.clojure/clojure "1.9.0-alpha14"]
   [org.clojure/clojurescript "1.9.229"]

   [org.clojure/tools.nrepl "0.2.12"]

   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.1" :scope "test"]

   ;; Server deps
   [aero "1.0.3"]
   [bidi "2.0.16"]
   [com.stuartsierra/component "0.3.2"]
   [hiccup "1.0.5"]
   [org.clojure/tools.namespace "0.2.11"]
   [prismatic/schema "1.1.3"]
   [yada "1.2.1" :exclusions [aleph manifold ring-swagger prismatic/schema]]

   [aleph "0.4.2-alpha8"]
   [manifold "0.1.6-alpha1"]
   [metosin/ring-swagger "0.22.10"]

   ;; App deps
   [reagent "0.6.1"]
   [com.cognitect/transit-clj "0.8.297"]
   [cljsjs/material "1.3.0-0"]
   [hiccups "0.3.0"]

   ;; Logging
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.21"]
   [org.slf4j/jul-to-slf4j "1.7.21"]
   [org.slf4j/log4j-over-slf4j "1.7.21"]
   [ch.qos.logback/logback-classic "1.1.5" :exclusions [org.slf4j/slf4j-api]]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-reload :refer [reload]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[ping.system :refer [new-system]])

(task-options!
 repl {:client true
       :port repl-port}
 pom {:project (symbol project)
      :version version
      :description "Diagnostic start-of project"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}}
 aot {:namespace #{'ping.main}}
 jar {:main 'ping.main
      :file (str project "-app.jar")})

(deftask dev-system
  "Identical to yada's Edge, the server backend is automatically started in
  the dev profile."
  []
  (with-pass-thru _
    (require 'reloaded.repl)
    (let [go (resolve 'reloaded.repl/go)]
      (try
        (require 'user)
        (go)
        (catch Exception e
          (boot.util/fail "Exception while starting the system\n")
          (boot.util/print-ex e))))))

(deftask dev
  "main development entry point."
  []
  (set-env! :dependencies #(vec (concat % '[[reloaded.repl "0.2.1"]])))
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (watch)
   (speak)
   (reload :on-jsload 'ping.main/init)
   (cljs-repl :nrepl-opts {:client false
                           :port repl-port
                           :init-ns 'user}) ; this is also the server repl!
   (cljs :ids #{"ping"} :optimizations :none)
   (dev-system)
   (target)))

(deftask static
  "This is used for creating optimized static resources under static"
  []
  (cljs :ids #{"ping"} :optimizations :advanced))

(deftask build
  []
  (comp
   (static)
   (target :dir #{"static"})))

(deftask run-system
  [p profile VAL str "Profile to start system with"]
  (with-post-wrap fileset
    (println "Running system with profile" profile)
    (let [system (new-system profile)]
      (component/start system)
      (intern 'user 'system system)
      (assoc fileset :system system))))

(deftask run [p profile VAL kw "Profile"]
  (comp
   (repl :server true
         :port (case profile :prod 5601 :beta 5602 5600)
         :init-ns 'user)
   (run-system (or profile :prod))
   (wait)))

(deftask uberjar
  "Build an uberjar"
  []
  (println "Building uberjar")
  (comp
   (static)
   (aot)
   (pom)
   (uber)
   (jar)
   (target)))

;; @Jon: you might need this for AWS deployment

;; (def environment-name (str project "-prod"))
;; (def aws-region "eu-west-1")
;; (def aws-account-id "247806367507")
;; (def zipfile (format "%s-%s.zip" project version))

;; (deftask create-ebs
;;   "Create AWS Beanstalk application and environment, only call this once."
;;   []
;;   (println "Creating application:" project)
;;   (dosh "aws" "elasticbeanstalk" "create-application"
;;         "--application-name" project)
;;   (println "Creating environment:" project environment-name)
;;   (dosh "aws" "elasticbeanstalk" "create-environment"
;;         "--application-name" project
;;         "--environment-name" environment-name
;;         "--cname-prefix" environment-name
;;         "--solution-stack-name" "64bit Amazon Linux 2016.03 v2.1.6 running Docker 1.11.2"))

;; (deftask deploy-ebs "Deploy application to AWS elasticbeanstalk environment" []
;;   (println "Building zip file:" zipfile)
;;   (dosh "zip"
;;         (str "target/" zipfile)
;;         "Dockerfile"
;;         (str "target/" project "-app.jar")
;;         (str "target"))
;;   (println "Uploading zip file to S3:" zipfile)
;;   (dosh "aws" "s3" "cp" (str "target/" zipfile)
;;         (format "s3://elasticbeanstalk-%s-%s/%s" aws-region aws-account-id zipfile))
;;   (println "Creating application version:" version)
;;   (dosh "aws" "elasticbeanstalk" "create-application-version"
;;         "--application-name" project
;;         "--version-label" version
;;         "--source-bundle" (format "S3Bucket=elasticbeanstalk-%s-%s,S3Key=%s" aws-region aws-account-id zipfile))
;;   (println "Updating environment:" environment-name "->" version)
;;   (dosh "aws" "elasticbeanstalk" "update-environment"
;;         "--application-name" project
;;         "--environment-name" environment-name
;;         "--version-label" version)
;;   (println "Done!"))

;; (deftask ebs "Build uberjar and deploy it to AWS elasticbeanstalk" []
;;   (uberjar)
;;   (deploy-ebs))

;; (deftask show-version "Show version" [] (println version))
