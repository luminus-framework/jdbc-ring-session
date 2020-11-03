(defproject jdbc-ring-session "1.4.0"
  :description "Ring JDBC Session Store"
  :url "https://github.com/yogthos/jdbc-ring-session"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
 :dependencies [[org.clojure/clojure "1.10.1"]
                [ring/ring-core "1.8.2"]
                [com.taoensso/nippy "2.4.0"]
                [commons-codec/commons-codec "1.15"]
                [org.clojure/java.jdbc "0.7.11"]]

  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.4.200"]]}})
