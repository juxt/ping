(ns ping.comm
  (:require [clj-http.client :as http]))

;; TODO: add more known failure exceptions
(defn call-diagnose-server [diagnose-server]
  (let [raw-diag {:result nil :failure nil :server diagnose-server}]
    (try
      (assoc raw-diag :result (http/get diagnose-server))
      (catch java.net.UnknownHostException e
        (assoc raw-diag :failure "Unknown Host Exception"))
      (catch java.net.ConnectException e
        (assoc raw-diag :failure "Connection refused"))
      (catch Exception e
        (assoc raw-diag :failure (str e))))))

;; TODO: move to test section soon
(def mock-failure
  {:result nil
   :faiure "Connection refused"
   :server "dummyserver"})

(def mock-success
  {:result
   {:status 200
    :result {:body "<html></html>"}}
   :faiure nil
   :server "dummyserver"})

(def mock-error
  {:result
   {:status 403
    :result {:body "<html>Forbidden</html>"}}
   :faiure nil
   :server "dummyserver"})

(defn mock []
  (let [opt [mock-failure mock-error mock-success]]
    (rand-nth opt)))

(defn call-diagnose-server-mock []
  (mock))
