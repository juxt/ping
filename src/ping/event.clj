(ns ping.event
  (:require
   [clojure.core.async :refer [go >! tap chan]]
   [clojure.data.json :as json :refer [write-str]]
   [ping.state :as state]))

(def events-queue (atom '()))

(defn build [time data-map]
  (let [ev (select-keys data-map [:status])]
    (assoc ev :msg time)))

(defn store [event]
  (swap! state/event-history conj event)
  (swap! events-queue conj event)
  event)

(defn update-queue []
  (swap! events-queue pop))

(defn send [channel multiple event]
  (println "sending " event)
  (tap multiple channel)
  (go (>! channel (json/write-str event)))
  channel)

(defn send-history [channel]
  (go (>! channel (json/write-str @state/event-history))))

(defn send-event [channel multiple & args]
  (let [event (peek @events-queue)
        _ (println "should send event: " event)]
    (if event
      (do (update-queue)
          (send channel multiple event))
      channel)))
