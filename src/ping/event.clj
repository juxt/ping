(ns ping.event
  (:require
   [clojure.core.async :refer [go >!]]
   [clojure.data.json :as json :refer [write-str]]))

(def history (atom []))

(defn build [time status]
  {:time time
   :status status})

(defn store [event]
  (swap! history conj event)
  event)

(defn send [channel event]
  (go (>! channel (json/write-str event))))

(defn send-history [channel]
  (go (>! channel (json/write-str @history))))
