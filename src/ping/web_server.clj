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
   [clojure.core.async :refer [go >! chan tap mult]]
   [ping.event :as event]))

(defn- send-event-history [component]
  (event/send-history (:channel component))
  (str @event/history))

(defn- send-event [component]
  (let [channel (:channel component)
        multiple (:mult component)
        ;; TODO: remove dummy event
        event (event/build "2016-08-03" "Failure")]
    (tap multiple channel)
    (event/store event)
    (event/send channel event)
    channel))

(defn content-routes [component]
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :ping.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response "test index"}}})]

    ["testing"
     (yada/resource
      {:id :ping.resource/testing
       :methods
       {:get
        {:produces #{"text/text"}
         :response (fn [ctx]
                     (send-event-history component))}}})]
    ["events/events"
     (yada/resource
      {:id :ping.resources/events
       :methods
       {:get
        {:produces #{"text/event-stream"}
         :response (fn [ctx]
                     (send-event component))}}})]
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
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   [:port]))
