;; Copyright © 2016, JUXT LTD.

(ns cljs.ping.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljsjs.material]
   [reagent.core :as r]
   [cljs.ping.state :as s]
   [cljs.reader :refer [read-string]]
   [cljs.core.async :refer [chan >! <!]]))

;; helper functions & setup
(def upgrade-dom (.. js/componentHandler -upgradeDom))

(defn mdl [& children]
  (r/create-class
   {:display-name "mdl-wrapper"
    :component-did-mount (fn [] (upgrade-dom))
    :component-did-update (fn [] (upgrade-dom))
    :reagent-render (fn [& children] (into [:div] children))}))

(defn render-id [comp-fn id]
  (r/render-component [comp-fn]
                      (.getElementById js/document id)))

(defn init-container [main]
  (render-id main "container"))


;; content ction


;; * state
(def state s/model)
(def keyn (r/atom 0))

;; * layout
(defn layout [content-fn]
  [mdl
   [:div.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
    [:main.mdl-layout__content
     (apply conj [:div.page-content {:id "pagecontent"}]
            [(content-fn)])]]])

(defn display-infos []
  [:div {:id "infodisplay"}])

;; * table
(defn list-info [msg status]
  (let [li [:li {:key @keyn} (str msg "|" status)]]
    (swap! keyn inc)
    li))

(defn ulist-infos [rows]
  (apply conj [:ul] [rows]))

(defn compose-infos
  ([] (apply compose-infos (-> state deref :diagnostics)))
  ([& inputs]
   (ulist-infos (map (fn [{msg :msg status :status :as input}]
                       (list-info msg status))
                     inputs))))

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
                           (js/alert "MESSAGE RECEIVED")
                           (put channel (process e read-string)))))
    (go-loop []
      (when-let [input (<! channel)]
        (js/alert "NEW MESSAGE ON CHANNEL")
        ;; update model
        (apply s/add-state ((juxt :msg :status) input))
        ;; render component
        (render-id comp-fn id))
      (recur))))


;; * test-out rendering
(defn run []
  (s/add-state "new-date" "OK")
  (render-id compose-infos "infodisplay")
  (js/setTimeout #(run) 2000))


;; init section where the magic happens

(defn init []
  (init-container #(layout display-infos))
  (render-id compose-infos "infodisplay")
  (js/setTimeout #(listen "infodisplay" compose-infos "http://localhost:3000/events/events" (chan 10)) 5000)
  ;(js/setTimeout #(run) 2000)
  )
