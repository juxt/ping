(ns cljs.ping.listener
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljsjs.material]
   [reagent.core :as r]
   [cljs.ping.state :as s]
   [cljs.ping.widget :as w]
   [cljs.reader :refer [read-string]]
   [cljs.core.async :refer [chan >! <!]]))

;; * listening
(defn process [e f]
  (-> e .-data f))

(defn json-to-map [json]
  (js->clj (.parse js/JSON json) :keywordize-keys true))

(defn put [channel data]
  (go (>! channel data)))

(defn- add-state [input-map]
  (apply s/add-state ((juxt :msg :status) input-map)))

(defn- add-history [input-vec]
  (loop [im input-vec]
    (if (= 0 (count im))
      ""
      (do
        (add-state (first im))
        (recur (rest im))))))

(defn listen [id comp-fn url channel]
  (let [srev (new js/EventSource url)]
    (.addEventListener srev "message"
                       (fn [e]
                         (do
          ;                  (js/alert (str "MESSAGE RECEIVED" (.-data e)))
                           (put channel (process e json-to-map)))))
    (go-loop []
      (when-let [input (<! channel)]
        ; (js/alert (str "NEW MESSAGE ON CHANNEL" input))

         (if (map? input)
           (add-state input)
           (add-history input))

        ;; render component
        (w/render-id comp-fn id))
      (recur))))
