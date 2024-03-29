(ns jdbc-ring-session.core
  (:require [taoensso.nippy :as nippy]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as jdbc.sql]
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

(defn serialize-sqlite [value]
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

(defn deserialize-sqlite [value]
  (when value
    (nippy/thaw value)))

(def serializers
  {:mysql     serialize-mysql
   :postgres  serialize-postgres
   :oracle    serialize-oracle
   :h2        serialize-h2
   :sqlserver serialize-sqlserver
   :sqlite    serialize-sqlite})

(def deserializers
  {:mysql     deserialize-mysql
   :postgres  deserialize-postgres
   :oracle    deserialize-oracle
   :h2        deserialize-h2
   :sqlserver deserialize-sqlserver
   :sqlite    deserialize-sqlite})

(defn detect-db [db-spec]
  (let [db-name (with-open [conn (jdbc/get-connection db-spec)]
                  (.. conn getMetaData getDatabaseProductName toLowerCase))]
    (cond
      (.contains db-name "oracle") :oracle
      (.contains db-name "postgres") :postgres
      (.contains db-name "mysql") :mysql
      (.contains db-name "mariadb") :mysql
      (.contains db-name "h2") :h2
      (.contains db-name "sql server") :sqlserver
      (.contains db-name "sqlite") :sqlite
      :else (throw (Exception. (str "unrecognized DB: " db-name))))))

(defn read-session-value [datasource table deserialize key]
  (jdbc/with-transaction [tx datasource]
    (-> (jdbc/execute-one! tx [(str "select value from " (name table) " where session_id = ?") key])
        (vals)
        (first)
        deserialize)))

(defn update-session-value! [tx table serialize key value]
  (let [data {:idle_timeout     (:ring.middleware.session-timeout/idle-timeout value)
              :absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
              :value            (serialize value)}
        updated (jdbc.sql/update! tx (name table) data {:session_id key})]
    (when (zero? (:next.jdbc/update-count updated))
      (jdbc.sql/insert! tx table (assoc data :session_id key)))
    key))

(defn insert-session-value! [tx table serialize value]
  (let [key (str (UUID/randomUUID))
        data {:session_id key
              :idle_timeout     (:ring.middleware.session-timeout/idle-timeout value)
              :absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
              :value            (serialize value)}]
    (jdbc.sql/insert! tx table data)
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
    (next.jdbc/with-transaction [tx datasource]
      (jdbc.sql/delete! tx table {:session_id key}))
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
