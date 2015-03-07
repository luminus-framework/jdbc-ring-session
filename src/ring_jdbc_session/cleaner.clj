(ns ring-jdbc-session.cleaner
  (:require [clojure.java.jdbc :as jdbc]))

(defn remove-sessions [conn]
  (let [t (System/currentTimeMillis)]
    (jdb/delete! db :session_store ["? - idle_timeout < 0 or ? - absolute_timeout < 0" t t])))

(defprotocol Stoppable
  "Something that can be stopped"
  (stopped? [_] "Return true if stopped, false otherwise")
  (stop     [_] "Stop (idempotent)"))

(defn sleep [millis]
  (let [timeout (+ millis (System/currentTimeMillis))]
    (while (< (System/currentTimeMillis) timeout)
      (try (Thread/sleep (- timeout (System/currentTimeMillis)))
        (catch InterruptedException _
          (.interrupt ^Thread (Thread/currentThread)))))))

(defn start-cleaner
  ([db]
    (start-cleaner db {}))
  ([db {:keys [interval-secs]
        :or {interval-secs 60}}]
    (let [state-atom  (atom :running)
          interval-ms (* 1000 interval-secs)
          runner      ]
      (-> (fn runner []
            (when @state-atom
              (remove-sessions db)
              (sleep interval-ms)))
          (Thread.)
          (.start))
      (reify Stoppable
        (stopped? [_] (not @state-atom))
        (stop     [_] (swap! state-atom (constantly false)))))))

(defn stop-cleaner [session-cleaner]
  {:pre [(satisfies? Stoppable session-cleaner)]}
  (.stop session-cleaner))
