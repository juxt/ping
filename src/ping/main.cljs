;; Copyright © 2016, JUXT LTD.

(ns ping.main
  (:require
   [cljsjs.material]
   [reagent.core :as r]))

(def upgrade-dom (.. js/componentHandler -upgradeDom))

(defn mdl [& children]
  (r/create-class
   {:display-name "mdl-wrapper"
    :component-did-mount (fn [] (upgrade-dom))
    :component-did-update (fn [] (upgrade-dom))
    :reagent-render (fn [& children] (into [:div] children))}))

(defn render-main [main-fn doc-id]
  (r/render-component [main-fn]
                      (.getElementById js/document doc-id)))

(defn init-container [main]
  (render-main main "container"))

(defn compose [content-fn]
  [mdl
   [:div.mdl-layout.mdl-js-layout.mdl-layout--fixed-header
    [:main.mdl-layout__content
     (apply conj [:div.page-content] ;; overkill rulez
            [(content-fn)])]]])

(defn content []
  [:h4.mdl-typography--text-center.mdl-typography--text-center.mdl-typography--display-2.mdl-typography--font-thin "Hello Ping!"
   ])

(defn run []
  (compose content))

(defn init []
  (init-container #(compose content)))
