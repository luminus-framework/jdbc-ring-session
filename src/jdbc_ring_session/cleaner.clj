(ns jdbc-ring-session.cleaner
  (:require [clojure.java.jdbc :as jdbc])
  (:import [java.util.concurrent Executors TimeUnit ScheduledExecutorService]))

(defn remove-sessions
  "removes stale sessions from the session table"
  [conn {:keys [table]
         :or   {table :session_store}}]
  (let [t (quot (System/currentTimeMillis) 1000)]
    (jdbc/delete! conn table ["idle_timeout < ? or absolute_timeout < ?" t t])))

(defprotocol Stoppable
  "Something that can be stopped"
  (stopped? [_] "Return true if stopped, false otherwise")
  (stop [_] "Stop (idempotent)"))

(defn start-cleaner
  "starts a session cleaner
   conn - database connection
   config - configuration map that ring-jdbc-session was initialized with"
  ([conn] (start-cleaner conn {}))
  ([conn {:keys [interval]
          :or   {interval 60}
          :as   config}]
   (let [scheduler ^ScheduledExecutorService (Executors/newScheduledThreadPool 1)]
     (.scheduleWithFixedDelay scheduler
                              (fn [] (remove-sessions conn config))
                              0
                              (long interval)
                              TimeUnit/SECONDS)

     (reify Stoppable
       (stopped? [_] (.isShutdown scheduler))
       (stop [_] (.shutdown scheduler))))))

(defn stop-cleaner
  "stops the instance of the session cleaner"
  [session-cleaner]
  {:pre [(satisfies? Stoppable session-cleaner)]}
  (.stop session-cleaner))
