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
   [clojure.core.async :refer [chan mult]]
   [ping.event :as event]
   [ping.comm :as comm]
   [ping.parser :as parser]
   [ping.state :as state]
   [ping.core :as core]))

(defn- send-event-history [{:keys [channel]}]
  (event/send-history channel)
  (str @state/event-history))

(defn- event-flow [channel mult tick-time]
  (let [timestamp (core/extract-datetime tick-time)
        _ (println " timestamp: " timestamp)]
    (->>
     ;; TODO: replace with: (comm/call-diagnose-server)
     (comm/call-diagnose-server-mock)
     (parser/parse)
     (event/build timestamp)
     (event/store)
     (event/send-event channel mult))))

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
                     (do
                       ;;TODO: read the hard-coded value of 1 min from edn file
                       (future (core/scheduler-flow
                                (partial event-flow (:channel component) (:mult component)) 1))

                       (io/file "assets/index.html")))}}})]

    ["testing"
     (yada/resource
      {:id :ping.resource/testing
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (send-event-history component))}}})]
    ["events/events"
     (yada/resource
      {:id :ping.resources/events
       :methods
       {:get
        {:produces #{"text/event-stream"}
         :response (fn [ctx]
                     (event/send-event (:channel component)
                                       (:mult component)))}}})]
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
