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

(defn put [channel data]
  (go (>! channel data)))

(defn listen [id comp-fn url channel]
  (let [srev (new js/EventSource url)]
    (.addEventListener srev "message"
                       (fn [e]
                         (do
                           ;; (js/alert (str "MESSAGE RECEIVED" (.-data e)))
                           ;; TODO: change the read-string to a more able parser;
                           (put channel (process e read-string)))))
    (go-loop []
      (when-let [input (<! channel)]
        ;; (js/alert "NEW MESSAGE ON CHANNEL")
        ;; update model
        (apply s/add-state ((juxt :msg :status) input))
        ;; render component
        (w/render-id comp-fn id))
      (recur))))
