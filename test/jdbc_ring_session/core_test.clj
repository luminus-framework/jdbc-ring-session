(ns jdbc-ring-session.core-test
  (:require [clojure.test :refer :all]
            [next.jdbc :as jdbc]
            [jdbc-ring-session.core :refer :all]))

#_(def db
  {:classname   "org.h2.Driver"
   :subprotocol "h2"
   :subname     "./test/session.db"
   :naming         {:keys   clojure.string/lower-case
                    :fields clojure.string/upper-case}})

(def db {:dbtype "h2" :dbname "example"})

(defn create-test-table [db]
  (jdbc/with-transaction [tx db]
    (jdbc/execute! tx ["drop table if exists session_store"])
    (jdbc/execute! tx  ["
CREATE TABLE session_store (
  session_id VARCHAR(36) NOT NULL,
  idle_timeout BIGINT DEFAULT NULL,
  absolute_timeout BIGINT DEFAULT NULL,
  value BINARY(10000),
  PRIMARY KEY (session_id)
 )"])))

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

(deftest test-delete-session-transaction
  (let [store (-> db
                  ;; disable auto-commit
                  (assoc-in [:options :auto-commit] false)
                  (jdbc-store))
        data  {:foo "bar" :bar [1 2 3]}
        k     (.write-session store nil data)]

    (testing "Delete should be run in transaction because the connection can have disabled auto-commit"
      (is (= data (.read-session store k)))
      (is (nil? (.delete-session store k)))
      (is (nil? (.read-session store k))))))