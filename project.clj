(defproject jdbc-ring-session "0.4"
  :description "Ring JDBC Session Store"
  :url "https://github.com/yogthos/jdbc-ring-session"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
 :dependencies [[org.clojure/clojure "1.6.0"]
                [ring/ring-core "1.3.2"]
                [com.taoensso/nippy "2.9.0"]
                [commons-codec/commons-codec "1.10"]
                [org.clojure/java.jdbc "0.3.6"]]

  :profiles
  {:dev {:dependencies [[postgresql/postgresql "9.1-901-1.jdbc4"]]}})
