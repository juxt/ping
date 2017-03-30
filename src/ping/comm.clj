(ns ping.comm
  (:require
   [clojure.core.async :as a]))

(def state {:channel (a/chan)
            :history (atom [])})

(defn build-event [timestamp status info]
  {:timestamp timestamp
   :info info
   :status status})

(defn store-event [event]
  (swap! (:history state) conj event)
  event)

(defn send-event [event]
  (println "Send update event:" event )
  (a/put! (:channel state) event))

(defn send-events [event]
  (println "Send all history" (-> state :history deref))
  (a/put! (:channel state) (-> state :history deref)))
