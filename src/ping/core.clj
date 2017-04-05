(ns ping.core
  (:require [tick.core :refer [minutes]]
            [tick.timeline :refer [timeline periodic-seq]]
            [tick.clock :refer [now clock-ticking-in-seconds]]
            [tick.schedule :refer [schedule start stop]]
            [ping.state :as state]))

(defn- create-timeline [interval]
  (timeline (periodic-seq (now) (minutes interval))))

(defn- create-scheduler [f tline]
  (schedule f tline))

(defn- store-scheduler [sch]
  (swap! state/schedulers conj sch))

(defn- start-scheduler [sch]
  @(start sch (clock-ticking-in-seconds)))

(defn extract-datetime [tick-date]
  (-> tick-date :tick/date .toLocalDateTime .toString))

(defn scheduler-flow [f interval]
  (let [tline (create-timeline interval)
        sch (create-scheduler f tline)]
    (store-scheduler sch)
    (start-scheduler sch)))

(defn stop-schedulers []
  (map #(stop %) @state/schedulers))
