;; Copyright © 2016, JUXT LTD.

(ns cljs.ping.main
  (:require
   [cljsjs.material]
   [reagent.core :as r]
   [cljs.ping.state :as s]
   [cljs.ping.widget :as w]
   [cljs.ping.listener :as l]
   [cljs.core.async :refer [chan]]))

;; * state
(def state s/model)
(def keyn (r/atom 0))

;; * compose ui
(defn compose-infos
  ([] (when (-> state deref :diagnostics empty? not)
        (apply compose-infos (-> state deref :diagnostics))))
  ([& inputs]
   (apply w/ping-table (map (fn [{msg :msg status :status :as input}]
                              (w/table-row (w/table-data msg :mdl-data-table__cell--non-numeric :juxt-font)
                                           (w/table-data status :juxt-inverted-font)))
                            (vec inputs)))))

;; * test-out rendering
(defn run []
  (s/add-state "1982/01/13" "OK")
  (w/render-id compose-infos "infodisplay")
  (js/setTimeout #(run) 2000))


;; init section where the magic happens
(defn init []
  (w/init-container #(w/layout w/display-infos))
  (w/render-id compose-infos "infodisplay")
  ;; (js/setTimeout #(do (apply s/add-state ((juxt :msg :status) {:msg "17/12/17" :status "NOK"}))
  ;;                     (js/alert (str @state))
  ;;                     (render-id compose-infos "infodisplay")) 2000)
  (js/setTimeout #(l/listen "infodisplay" compose-infos "http://localhost:3000/events/events" (chan 10)) 5000))
