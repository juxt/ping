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

(defn setup-scheduler-flow [channel ping-interval]
  (future (tick/scheduler-flow (partial event-flow channel) ping-interval)))

(defn stop-schedulers []
  (tick/stop-schedulers))

(defn register-channel [mult]
  (event/create-channel mult))

(defn send-history [history-size channel]
  (event/send-history history-size channel))
