# jdbc-ring-session

A Clojure library for using a SQL backend for Ring sessions.

[![Clojars Project](http://clojars.org/jdbc-ring-session/latest-version.svg)](http://clojars.org/jdbc-ring-session)

## Usage

Use the `ring-jdbc-session.core/jdbc-store` function to create a new store. The function accepts
a `clojure.java.jdbc` datasource definition:

```clojure
(ns db.core
  (:require [ring-jdbc-session.core :refer [jdbc-store]]))

(def db
  {:subprotocol "postgresql"
   :subname "session"
   :user "admin"
   :password "admin"})
   
(def store (jdbc-store db))  
```

The session will be stored as a byte array serialized using [nippy](https://github.com/ptaoussanis/nippy). The table format for PostgreSQL is shown below:

```sql
CREATE TABLE session_store
(
  key VARCHAR(36),
  idle_timeout BIGINT,
  absolute_timeout BIGINT,
  value BYTEA
)
```

The `jdbc-store` function accepts optional map with keys called `:table` and `:blob-reader`. The `:table` defaults to `:session_store`, while the `:blob-reader` is used to specify a custom byte array reader for the specific database. The library will attempt to figure out the appropriate reader based on type. Currently, PostgeSQL and Oracle BLOB formats are supported as the default readers.

```clojure
(jdbc-store db {:table :sessions})
```


A cleaner thread is provided in the `ring-jdbc-session.cleaner` for removing expired sessions from the database. The `idle_timeout` and `absolute_timeout` keys are expected to be populated by the [ring-session-timeout](https://github.com/ring-clojure/ring-session-timeout) library. These keys are used by the cleaner to remove stale sessions. The cleaner can be started and stopped as follows:

```clojure
(ns db.core
  (:require [ring-jdbc-session.cleaner :refer [start-cleaner stop-cleaner]))
  
(start-cleaner)

(stop-cleaner)
```

The `start-cleaner` function accepts an optional map with the `:interval-secs` key that defaults to 60. This is the number of seconds to sleep between runs.

```clojure
(start-cleaner {:interval-secs 120})
```



## License

Copyright © 2015 Yogthos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
