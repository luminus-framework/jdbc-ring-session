(ns jdbc-ring-session.core
  (:require [taoensso.nippy :as nippy]
            [next.jdbc :as jdbc]
            [ring.middleware.session.store :refer :all])
  (:import java.util.UUID
           org.apache.commons.codec.binary.Base64))

(defn serialize-mysql [value]
  (nippy/freeze value))

(defn serialize-postgres [value]
  (nippy/freeze value))

(defn serialize-oracle [value]
  (-> value nippy/freeze Base64/encodeBase64))

(defn serialize-h2 [value]
  (nippy/freeze value))

(defn serialize-sqlserver [value]
  (nippy/freeze value))

(defn deserialize-mysql [value]
  (when value
    (nippy/thaw value)))

(defn deserialize-postgres [value]
  (when value
    (nippy/thaw value)))

(defn deserialize-oracle [blob]
  (when blob
    (-> blob (.getBytes 1 (.length blob)) Base64/decodeBase64 nippy/thaw)))

(defn deserialize-h2 [value]
  (when value
    (nippy/thaw value)))

(defn deserialize-sqlserver [value]
  (when value
    (nippy/thaw value)))


(def serializers
  {:mysql     serialize-mysql
   :postgres  serialize-postgres
   :oracle    serialize-oracle
   :h2        serialize-h2
   :sqlserver serialize-sqlserver})

(def deserializers
  {:mysql     deserialize-mysql
   :postgres  deserialize-postgres
   :oracle    deserialize-oracle
   :h2        deserialize-h2
   :sqlserver deserialize-sqlserver})

(defn detect-db [db-spec]
  (let [db-name (with-open [conn (jdbc/get-connection db-spec)]
                  (.. conn getMetaData getDatabaseProductName toLowerCase))]
    (cond
      (.contains db-name "oracle") :oracle
      (.contains db-name "postgres") :postgres
      (.contains db-name "mysql") :mysql
      (.contains db-name "h2") :h2
      (.contains db-name "sql server") :sqlserver
      :else (throw (Exception. (str "unrecognized DB: " db-name))))))

(defn read-session-value [datasource table deserialize key]
  (jdbc/with-transaction [tx datasource]
    (-> (jdbc/execute-one! tx [(str "select value from " (name table) " where session_id = ?") key])
        :SESSION_STORE/VALUE
        deserialize)))

(defn update-session-value! [tx table serialize key value]
  (let [idle_timeout     (:ring.middleware.session-timeout/idle-timeout value)
        absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
        value            (serialize value)
        updated (jdbc/execute-one! tx [(str "update " (name table) " set idle_timeout = ?, absolute_timeout = ?, value = ? where session_id = ?")
                                   idle_timeout
                                   absolute_timeout
                                   value
                                   key])]
    (when (zero? (:next.jdbc/update-count updated))
      (jdbc/execute! tx [(str "insert into " (name table) " (session_id, idle_timeout, absolute_timeout, value) VALUES (?, ?, ?, ?)")
                         key idle_timeout absolute_timeout value]))
    key))

(defn insert-session-value! [tx table serialize value]
  (let [key (str (UUID/randomUUID))
        idle_timeout     (:ring.middleware.session-timeout/idle-timeout value)
        absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
        value            (serialize value)]
    (jdbc/execute! tx [(str "insert into " (name table) " (session_id, idle_timeout, absolute_timeout, value) VALUES (?, ?, ?, ?)")
                       key idle_timeout absolute_timeout value])
    key))

(deftype JdbcStore [datasource table serialize deserialize]
  SessionStore
  (read-session
    [_ key]
    (read-session-value datasource table deserialize key))
  (write-session
    [_ key value]
    (next.jdbc/with-transaction [tx datasource]
      (if key
        (update-session-value! tx table serialize key value)
        (insert-session-value! tx table serialize value))))
  (delete-session
    [_ key]
    (jdbc/execute! datasource [(str "delete from " (name table) " where session_id = ?") key])
    nil))

(ns-unmap *ns* '->JdbcStore)

(defn jdbc-store
  ""
  [db-spec & [{:keys [table serialize deserialize]
               :or   {table :session_store}}]]
  (let [db-type     (detect-db db-spec)
        serialize   (or serialize (serializers db-type))
        deserialize (or deserialize (deserializers db-type))]
    (JdbcStore. db-spec table serialize deserialize)))
