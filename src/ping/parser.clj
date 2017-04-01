(ns ping.parser)

(def status #{:failure :error :success})

(defn- pretty-status [s]
  (-> s name (clojure.string/capitalize)))

(defn- build-diagnose
  ([server status]
   {:server server
    :status status})
  ([server status data]
   (conj (build-diagnose server status) {:data data})))

(defn- parse-success-or-error [server result]
  (let [build-fn (fn [status] (build-diagnose server status (:result result)))]
    (if (= 200 (:status result))
      (build-fn (pretty-status :success))
      (build-fn (pretty-status :error)))))

(defn- parse-failure [server]
  (build-diagnose server (pretty-status :failure)))

(defn parse [diagnose]
  (let [server (:server diagnose)]
    (if (:failure diagnose)
      (parse-failure server)
      (parse-success-or-error server (:result diagnose)))))
