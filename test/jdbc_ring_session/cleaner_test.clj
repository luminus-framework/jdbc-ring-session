(ns jdbc-ring-session.cleaner-test
  (:require [clojure.test :refer :all]
            [next.jdbc :as jdbc]
            [jdbc-ring-session.core :refer :all]
            [jdbc-ring-session.cleaner :refer :all]))

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

  (let [conn (jdbc/get-datasource db)
        store (jdbc-store conn)
        ten-seconds-ago (- (quot (System/currentTimeMillis) 1000) 10)
        data {:foo "bar"
              :bar [1 2 3]
              :ring.middleware.session-timeout/idle-timeout ten-seconds-ago
              :ring.middleware.session-timeout/absolute-timeout ten-seconds-ago}
        k    (.write-session store nil data)]

    (testing "Delete expired sessions"
      (is (= data (.read-session store k)))
      (remove-sessions conn {})
      (is (= nil (.read-session store k))))))
