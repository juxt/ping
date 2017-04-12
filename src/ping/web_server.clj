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
   [clojure.core.async :refer [chan mult]]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   [ping.core :as core]))

(defn- setup-scheduler [channel]
  (core/setup-scheduler-flow channel)
  (io/file "assets/index.html"))

(defn content-routes [component]
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :ping.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (setup-scheduler (:channel component)))}}})]

    ;; ["testing"
    ;;  (yada/resource
    ;;   {:id :ping.resource/testing
    ;;    :methods
    ;;    {:get
    ;;     {:produces #{"text/html"}
    ;;      :response (fn [ctx]
    ;;                  (send-event-history component))}}})]

    ["events/events"
     (yada/resource
      {:id :ping.resources/events
       :methods
       {:get
        {:produces #{"text/event-stream"}
         :response (fn [ctx]
                     (core/register-channel (:mult component)))}}})]

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

(s/defrecord WebServer [host :- s/Str
                        port :- s/Int
                        listener]
  Lifecycle
  (start [component]
    (let [channel (chan)
          component (assoc component :channel channel :mult (mult channel))]
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
    (core/stop-schedulers)
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   [:port]))
