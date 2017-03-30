;; Copyright © 2016, JUXT LTD.

(ns ping.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   [clojure.core.async :refer [go >! chan tap mult]]))

(def channel (chan 256))

(defn content-routes [component]
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :ping.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx] (io/file "assets/index.html"))}}})]
    
    ["testing"
     (yada/resource
      {:id :ping.resource/testing
       :methods
       {:get
        {:produces #{"text/text"}
         :response (fn [ctx]
                     (go (>! channel (str {:msg "MSG" :status "status"})))
                     (str "COMPOENT:" component))}}})]
    ["events/events"
     (yada/resource
      {:id :ping.resources/events
       :methods
       {:get
        {:produces #{"text/event-stream"}
         :response (fn [ctx]
                     (tap (:mult component) channel)
                     (go (>! channel (str {:msg "First" :status "MESSAGE"})))
                     channel)}}})]
    ["" (assoc (yada/redirect :ping.resources/index) :id :ping.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :ping.resources/static))]]])

(defn routes
  "Create the URI route structure for our application."
  [config component]
  [""
   [
    ;; Our content routes, and potentially other routes.
    (content-routes component)

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])

(s/defrecord WebServer [mhost :- s/Str
                        port :- s/Int
                        listener]
  Lifecycle
  (start [component]
    (let [component (assoc component :mult (mult channel))]
      (if listener
        component                         ; idempotence
        (let [vhosts-model
              (vhosts-model
               [{:scheme :http :host host}
                (routes {:port port} component)])
              listener (yada/listener vhosts-model {:port port})]
          (infof "Started web-server on port %s" (:port listener))
          (assoc component :listener listener)))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   [:port]))
