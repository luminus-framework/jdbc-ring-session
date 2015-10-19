(ns jdbc-ring-session.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [jdbc-ring-session.core :refer :all]))

(def db
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :subname     "./test/session.db"
   :naming         {:keys   clojure.string/lower-case
                    :fields clojure.string/upper-case}})

(defn create-test-table [db]
  (jdbc/with-db-transaction [t-conn db]
    (try
      (jdbc/db-do-commands
       t-conn
       (jdbc/drop-table-ddl :session_store))
      (jdbc/db-do-commands
       t-conn
       (jdbc/create-table-ddl
        :session_store
        [:session_id "VARCHAR(36) NOT NULL PRIMARY KEY"]
        [:idle_timeout :bigint]
        [:absolute_timeout :bigint]
        [:value "bytea"]))
      (catch Exception e
        (.getMessage (.getNextException e))))))

(use-fixtures
  :once
  (fn [f] (create-test-table db) (f)))

(deftest a-test
  (testing "test session write/read"
    (let [store (jdbc-store db)
          data {:foo "bar" :bar [1 2 3]}
          k    (.write-session store nil data)]
      (is (= data (.read-session store k))))))
