(defproject jdbc-ring-session "1.0"
  :description "Ring JDBC Session Store"
  :url "https://github.com/yogthos/jdbc-ring-session"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
 :dependencies [[org.clojure/clojure "1.8.0"]
                [ring/ring-core "1.6.2"]
                [com.taoensso/nippy "2.13.0"]
                [commons-codec/commons-codec "1.10"]
                [org.clojure/java.jdbc "0.7.0"]]

  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.4.196"]]}})
