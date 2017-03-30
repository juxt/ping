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
   [clojure.core.async :as a]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]
   [ping.comm :as comm]))

;; 1. call diagnose server
;; 2. call parser to interpret result (use dummy event to start with)
;; 3. save event to history
;; 4. send event OR event history to channel
;; 5. refresh client
(defn status-history-handler [ctx]
  (let [history (get-in ctx [:parameters :query :history])
        send-fn (fn[x] (if (Boolean/valueOf history)
                         (comm/send-events x)
                         (comm/send-event x)))]
    (->> (comm/build-event "2017-03-28" "some test" "OKish")
         (comm/store-event)
         (send-fn))
    (io/file "assets/index.html")))

(defn content-routes []
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :ping.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :parameters {:query {:history String}}
         :response status-history-handler}}})]

    ["" (assoc (yada/redirect :ping.resources/index) :id :ping.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :ping.resources/static))]]])

(defn routes
  "Create the URI route structure for our application."
  [config]
  [""
   [
    ;; Our content routes, and potentially other routes.
    (content-routes)

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])

(s/defrecord WebServer [host :- s/Str
                        port :- s/Int
                        listener]
  Lifecycle
  (start [component]
    (if listener
      component                         ; idempotence
      (let [vhosts-model
            (vhosts-model
             [{:scheme :http :host host}
              (routes {:port port})])
            listener (yada/listener vhosts-model {:port port})
            channel (a/chan)]
        (infof "Started web-server on port %s" (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   [:port]))
