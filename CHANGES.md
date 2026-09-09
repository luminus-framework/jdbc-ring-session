1.6.0 - [write-session no longer recreates a session that was deleted by a concurrent request](https://github.com/luminus-framework/jdbc-ring-session/issues/25)
      - a write against a session id whose row is gone (logout, revocation, or the cleaner) is now a no-op instead of re-inserting the row
1.5.2 - [SQLite support](https://github.com/luminus-framework/jdbc-ring-session/pull/21)
1.5.1 - [Moves query for removing expired sessions into a transaction](https://github.com/luminus-framework/jdbc-ring-session/pull/20)
1.5.0 - switch to use [next.jdbc](https://github.com/luminus-framework/jdbc-ring-session/pull/18)
      - breaking changes: database connection specification changed from clojure.java.jdbc to next.jdbc, see next.jdbc [migration guide](https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.2.724/doc/migration-from-clojure-java-jdbc#primary-api) for details
