(ns ping.core
  [:require
   [ping.tick :as tick]
   [ping.event :as event]
   [ping.parser :as parser]
   [ping.comm :as comm]])

(defn- event-flow [channel tick-time]
  (let [timestamp (tick/extract-datetime tick-time)]
    (->>
     ;; TODO: replace with: (comm/call-diagnose-server)
     (comm/call-diagnose-server-mock)
     (parser/parse)
     (event/build timestamp)
     (event/store)
     (event/send-event channel))))

(defn setup-scheduler-flow [channel]
  ;;TODO: read the hard-coded value of 1 min from edn file
  (future (tick/scheduler-flow (partial event-flow channel) 1)))

(defn stop-schedulers []
  (tick/stop-schedulers))

(defn register-channel [mult]
  (event/create-channel mult))

(defn send-history [channel]
  (event/send-history channel))
