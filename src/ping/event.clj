(ns ping.event
  (:require
   [clojure.core.async :refer [go >! tap chan]]
   [clojure.data.json :as json :refer [write-str]]))

(def history (atom []))

(defn build [time data-map]
  (let [ev (select-keys data-map [:status])]
    (assoc ev :msg time)))

(defn store [event]
  (swap! history conj event)
  event)

(defn send [channel multiple event]
  (tap multiple channel)
  (go (>! channel (json/write-str event)))
  channel)

(defn send-history [channel]
  (go (>! channel (json/write-str @history))))
