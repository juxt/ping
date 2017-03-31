(ns cljs.ping.widget
  (:require
   [cljsjs.material]
   [reagent.core :as r]))

;; helper functions & setup
(def upgrade-dom (.. js/componentHandler -upgradeDom))

(defn mdl [& children]
  (r/create-class
   {:display-name "mdl-wrapper"
    :component-did-mount (fn [] (upgrade-dom))
    :component-did-update (fn [] (upgrade-dom))
    :reagent-render (fn [& children] (into [:div] children))}))

;; header
(defn header []
  [:div.mdl-layout__header.mdl-layout__header--waterfall.juxth
   [:div.mdl-layout__header-row
    [:span.mdl-layout-title
     [:img.ping-logo-image {:src "juxt.png"}]]]])

(defn render-id [comp-fn id]
  (r/render-component [comp-fn]
                      (.getElementById js/document id)))

(defn init-container [main]
  (render-id main "container"))

;; footer

;; general layout
(defn layout [content-fn]
  [mdl
   [header]
   [:div.demo-blog.mdl-layout.mdl-js-layout.is-upgraded
    [:div.mdl-layout__content    
     [:div.ping.mdl-grid
      [:div.mdl-card.mdl-cell.mdl-cell--8-col       
        (apply conj [:div.mdl-card__text
                       {:id "pagecontent"}]
                 [(content-fn)])]]]]])

(defn display-infos []
  [:div {:id "infodisplay"}])

;; table
(def key-counter (r/atom 0))

(defn add-class [elem & classes]
  (keyword (reduce #(str %1
                         "." (name %2))
                   (name elem) classes)))

(defn table-elem [telem attrs data & classes]
  [(apply add-class telem classes) attrs data])

(defn table-hdata [item & classes]
  (apply table-elem :th {:key (swap! key-counter inc)} item classes))

(defn table-data [data & classes]
  (apply table-elem :td {:key (swap! key-counter inc)} data classes))

(defn table-row [& data]
  (apply conj [:tr {:key (swap! key-counter inc)}] data))

(defn table [head & rows]
  [:table.mdl-data-table.mdl-js-data-table.mdl-data-table--selectable.mdl-shadow--2dp
   [:thead head]
   (apply conj [:tbody] rows)])


(defn ping-table [& rows]
  (let [timelineh (table-hdata "Status time collection" :mdl-data-table__cell--non-numeric :mdl-typography--display-2 :mdl-typography--font-thin)
        statush (table-hdata "Status info" :mdl-typography--display-1 :mdl-typography--font-thin)
        header (table-row timelineh statush)
        rows (or rows (map #(identity (table-row (table-data "2017/03/31" :mdl-data-table__cell--non-numeric :juxt-font)
                                                 (table-data "OK" :juxt-inverted-font)))
                           (range 1 100));;TODO: remove the or; just for testing UI
                 )]
    (apply table header rows)))

;; others
(defn list-info [keyn msg status]
  (let [li [:li {:key @keyn} (str msg "|" status)]]
    (swap! keyn inc)
    li))

(defn ulist-infos [rows]
  (apply conj [:ul] [rows]))
