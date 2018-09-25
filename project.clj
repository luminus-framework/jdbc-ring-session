(defproject jdbc-ring-session "1.2"
  :description "Ring JDBC Session Store"
  :url "https://github.com/yogthos/jdbc-ring-session"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
 :dependencies [[org.clojure/clojure "1.9.0"]
                [ring/ring-core "1.7.0"]
                [com.taoensso/nippy "2.14.0"]
                [commons-codec/commons-codec "1.11"]
                [org.clojure/java.jdbc "0.7.8"]]

  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.4.197"]]}})
