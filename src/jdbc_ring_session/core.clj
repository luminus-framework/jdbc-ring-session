(ns jdbc-ring-session.core
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.nippy :as nippy]
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

(def serializers
  {:mysql serialize-mysql
   :postgres serialize-postgres
   :oracle serialize-oracle
   :h2 serialize-h2})

(def deserializers
  {:mysql deserialize-mysql
   :postgres deserialize-postgres
   :oracle deserialize-oracle
   :h2 deserialize-h2})

(defn detect-db [db]
  (let [db-name (.. (jdbc/get-connection db) getMetaData getDatabaseProductName toLowerCase)]
    (cond
     (.contains db-name "oracle") :oracle
     (.contains db-name "postgres") :postgres
     (.contains db-name "mysql") :mysql
     (.contains db-name "h2") :h2
     :else (throw (Exception. (str "unrecognized DB: " db-name))))))

(defn read-session-value [datasource table deserialize key]
  (jdbc/with-db-transaction [conn datasource]
    (-> (jdbc/query conn [(str "select value from " (name table) " where session_id = ?") key])
        first :value deserialize)))

(defn update-session-value! [conn table serialize key value]
  (jdbc/update!
   conn
   :session_store {:idle_timeout (:ring.middleware.session-timeout/idle-timeout value)
                   :absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
                   :value (serialize value)}
   ["session_id = ? " key])
  key)

(defn insert-session-value! [conn table serialize value]
  (let [key (str (UUID/randomUUID))]
    (jdbc/insert!
     conn
     :session_store {:session_id key
                     :idle_timeout (:ring.middleware.session-timeout/idle-timeout value)
                     :absolute_timeout (:ring.middleware.session-timeout/absolute-timeout value)
                     :value (serialize value)})
    key))

(deftype JdbcStore [datasource table serialize deserialize]
  SessionStore
  (read-session
   [_ key]
   (read-session-value datasource table deserialize key))
  (write-session
   [_ key value]
   (jdbc/with-db-transaction [conn datasource]
     (if key
       (update-session-value! conn table serialize key value)
       (insert-session-value! conn table serialize value))))
  (delete-session
   [_ key]
   (jdbc/delete! datasource table ["session_id = ?" key])
   nil))

(ns-unmap *ns* '->JdbcStore)

(defn jdbc-store [db & [{:keys [table serialize deserialize]
                         :or {table :session_store}}]]
  (let [db-type (detect-db db)
        serialize (or serialize (serializers db-type))
        deserialize (or deserialize (deserializers db-type))]
    (JdbcStore. db table serialize deserialize)))
