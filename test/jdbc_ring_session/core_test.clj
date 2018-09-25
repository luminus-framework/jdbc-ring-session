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
      (catch org.h2.jdbc.JdbcBatchUpdateException e
        (when-not (re-find #"(?i)table.+not found" (.getMessage e))
          (throw (ex-info "Could not reset session store table in test database" {} e)))))
    (jdbc/db-do-commands
      t-conn
      (jdbc/create-table-ddl
        :session_store
        [[:session_id "VARCHAR(36) NOT NULL PRIMARY KEY"]
         [:idle_timeout :bigint]
         [:absolute_timeout :bigint]
         [:value "bytea"]]))))

(use-fixtures
  :once
  (fn [f] (create-test-table db) (f)))

(deftest a-test
  (testing "test session write/read"
    (let [store (jdbc-store db)
          data {:foo "bar" :bar [1 2 3]}
          k    (.write-session store nil data)]
      (is (= data (.read-session store k)))

      (testing "same session-id is reused after it has expired (deleted)"
        (.delete-session store k)
        (.write-session store k data)
        (is (= data (.read-session store k)))))))
