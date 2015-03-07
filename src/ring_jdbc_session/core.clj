(ns ring-jdbc-session.core
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.nippy :as nippy]
            [ring.middleware.session.store :refer :all])
  (:import java.util.UUID))

(defn deserialize [result blob-reader]
  (when-let [value (-> result first :value)]
    (-> value blob-reader nippy/thaw)))

(defn oracle-blob-reader [blob]
  (when blob (.getBytes blob 1 (.length blob))))

(defn postgres-blob-reader [blob]
  blob)

(defn detect-blob-reader [db]
  (let [db-name (.. (jdbc/get-connection db) getMetaData getDatabaseProductName toLowerCase)]
    (cond
     (.contains db-name "oracle") oracle-blob-reader
     (.contains db-name "postgres") postgres-blob-reader
     :else (throw (Exception. (str "no BLOB reader available for: " db-name))))))

(defn read-session-value [datasource table blob-reader key]
  (jdbc/with-db-transaction [conn datasource]
    (deserialize
     (jdbc/query conn ["select value from session_store where key = ?" key])
     blob-reader)))

(defn update-session-value! [conn table key value]
  (jdbc/update!
   conn
   :session_store {:idle_timeout (:ring.middleware.session-timeout/idle-timeout value)
                   :absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
                   :value (nippy/freeze value)}
   ["key = ? " key])
  key)

(defn insert-session-value! [conn table value]
  (let [key (str (UUID/randomUUID))]
    (jdbc/insert!
     conn
     :session_store {:key key
                     :idle_timeout (:ring.middleware.session-timeout/idle-timeout value)
                     :absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
                     :value (nippy/freeze value)})
    key))

(deftype JdbcStore [datasource table blob-reader]
  SessionStore
  (read-session
   [_ key]
   (read-session-value datasource table blob-reader key))
  (write-session
   [_ key value]
   (jdbc/with-db-transaction [conn datasource]
     (if key
       (update-session-value! conn table key value)
       (insert-session-value! conn table value))))
  (delete-session
   [_ key]
   (jdbc/delete! datasource table ["key = ?" key])
   nil))

(ns-unmap *ns* '->JdbcStore)

(defn jdbc-store [db & [{:keys [table blob-reader]
                         :or {table :session_store
                              blob-reader (detect-blob-reader db)}}]]
  (JdbcStore. db table blob-reader))

