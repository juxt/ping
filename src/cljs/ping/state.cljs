(ns cljs.ping.state
  (:require [reagent.core :as r]
            [cljs.core.async :refer [>! <! chan]]))

(def model (r/atom {:channel (chan 20)
                    :diagnostics [{:msg "some-date" :status "some-status"}]}))

(defn create-status [msg status]
  {:msg msg :status status})

(defn update-model [mval k item]
  (let [nval (-> mval k (conj item))]
    (assoc mval k nval)))

(defn add-state
  ([msg status]
   (add-state model :diagnostics msg status))
  
  ([m k msg status]
   (swap! m #(update-model % k
                           (create-status msg status)))))
