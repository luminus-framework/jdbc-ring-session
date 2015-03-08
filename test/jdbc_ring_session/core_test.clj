(ns jdbc-ring-session.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [jdbc-ring-session.core :refer :all]))

(def db
  {:subprotocol "postgresql"
   :subname "session"
   :user "admin"
   :password "admin"})

(defn create-test-table [db]
  (try
    (jdbc/db-do-commands
     db (jdbc/create-table-ddl
         :session_store
         [:key "VARCHAR(36)"]
         [:idle_timeout :bigint]
         [:absolute_timeout :bigint]
         [:value "bytea"]))
    (catch Exception e
      (.getMessage (.getNextException e)))))

(deftest a-test
  (testing "test session write/read"
    (let [store (jdbc-store db)
          data {:foo "bar" :bar [1 2 3]}]
      (.write-session store nil data)
      (is (= data (.read-session store "3c5c6f73-1459-474e-8ced-a939c06f2cff"))))))
