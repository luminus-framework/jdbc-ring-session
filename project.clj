(defproject jdbc-ring-session "1.5.4"
  :description "Ring JDBC Session Store"
  :url "https://github.com/yogthos/jdbc-ring-session"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
 :dependencies [[org.clojure/clojure "1.11.1"]
                [ring/ring-core "1.9.6"]
                [com.taoensso/nippy "3.4.2"]
                [commons-codec/commons-codec "1.15"]
                [com.github.seancorfield/next.jdbc "1.3.847"]]

  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.4.200"]]}})
