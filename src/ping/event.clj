(ns ping.event
  (:require
   [clojure.core.async :refer [go >! tap chan]]
   [clojure.data.json :as json :refer [write-str]]
   [ping.state :as state]))

(defn build [time data-map]
  (let [ev (select-keys data-map [:status])]
    (assoc ev :msg time)))

(defn create-channel [mult]
  (let [ch (chan)]
    (tap mult ch)
    ch))

(defn store [event]
  (swap! state/event-history conj event)
  event)

(defn send-event [channel event]
  ;(println "sending event " event)
  (go (>! channel (json/write-str event)))
  channel)

(defn send-history [channel]
  (go (>! channel (json/write-str @state/event-history))))
