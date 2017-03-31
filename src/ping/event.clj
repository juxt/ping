(ns ping.event
  (:require
   [clojure.core.async :refer [go >!]]))

(def history (atom []))

(defn build [time status]
  {:time time
   :status status})

(defn store [event]
  (swap! history conj event)
  event)

(defn send [channel event]
  (go (>! channel (str event))))

(defn send-history [channel]
  (go (>! channel (str @history))))
