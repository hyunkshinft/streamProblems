# Stream Problems

When using `PGReplicationStream` from pgjdbc to get data off of a replication slot, we occasionally encounter an [`Unexpected packet type`](https://github.com/pgjdbc/pgjdbc/blob/REL42.2.5/pgjdbc/src/main/java/org/postgresql/core/v3/QueryExecutorImpl.java#L1236) while processing copy data. The control character that we are actually receiving is a lowercase ‘w’ which, when looking through other parts of the code, appears to be the control character that we’d receive when PostgreSQL is [telling pgjdbc to expect xlog data](https://github.com/pgjdbc/pgjdbc/blob/REL42.2.5/pgjdbc/src/main/java/org/postgresql/core/v3/replication/V3PGReplicationStream.java#L139).

99% of the time, this is fine since the replication is resilient and just kinda picks up where it left off after the exception is raised. We have, however, encountered times when, no matter what we did, reading from that stream always failed at some point during a transaction and the replication lag got rather out of hand. At first, we thought this had to do with the size of the transactions that were failing but it does not seem to make a difference how big these transactions are.

## Reproducing the error

This repository contains a toy app which simply uses pgjdbc and `PGReplicationStream` to listen for data from a replication slot in PostgreSQL. So that it has something to listen to, we also provided a PostgreSQL container which creates a table and a logical replication slot that the application can pull data off of. Following the steps below should give you a running example that, eventually, will produce the error. The container with the java app in it should exit and, at the very end of the log file, you should see something like:

```
2019-04-09T19:18:19.786087989Z  at org.postgresql.core.v3.QueryExecutorImpl.writeToCopy(QueryExecutorImpl.java:1000)
2019-04-09T19:18:19.786092483Z  at org.postgresql.core.v3.CopyDualImpl.writeToCopy(CopyDualImpl.java:19)
2019-04-09T19:18:19.786095396Z  at org.postgresql.core.v3.replication.V3PGReplicationStream.updateStatusInternal(V3PGReplicationStream.java:189)
2019-04-09T19:18:19.786098445Z  at org.postgresql.core.v3.replication.V3PGReplicationStream.timeUpdateStatus(V3PGReplicationStream.java:181)
2019-04-09T19:18:19.786101356Z  at org.postgresql.core.v3.replication.V3PGReplicationStream.readInternal(V3PGReplicationStream.java:121)
2019-04-09T19:18:19.786106196Z  at org.postgresql.core.v3.replication.V3PGReplicationStream.readPending(V3PGReplicationStream.java:78)
2019-04-09T19:18:19.786109569Z  at streamProblem.main(streamProblem.java:43)
2019-04-09T19:18:19.786186482Z Caused by: java.io.IOException: Unexpected packet type during copy: 119
2019-04-09T19:18:19.786213170Z  at org.postgresql.core.v3.QueryExecutorImpl.processCopyResults(QueryExecutorImpl.java:1236)
2019-04-09T19:18:19.786216840Z  at org.postgresql.core.v3.QueryExecutorImpl.writeToCopy(QueryExecutorImpl.java:998)
2019-04-09T19:18:19.786219961Z  ... 6 more
```

This typically takes about 15-20 minutes to happen following the instructions below, on some occasions it has taken an hour. One word of warning: make sure you have plenty of free disk space before running the command that starts inserting data into PostgreSQL. In about 15 minutes you might see about 40GB of disk use.

**Build, start up and daemonize the containers**

```
docker-compose build && docker-compose up -d
```

**Save the STDERR log from the container that is running the java app**

```
docker logs -f streamproblems_stream-problem_1 2> docker.log
```

**Unpack `data/break.csv.gz`. This is just a file full of `x`'s, to help simulate large messages in a single transaction**

```
gunzip data/break.csv.gz
```

**Start pumping large commits through PostgreSQL**

```
docker exec -it streamproblems_postgres_1 bash
while true; do psql -U test_user -h localhost -d test_database -c "begin; COPY test_rows(name) from '/data/break.csv'; commit;"; sleep 10; done
```

You'll know when the application has encountered the error because the `docker logs` command will exit. At this point it should be OK to interrupt the `while` loop that is copying data into PostgreSQL.

## Proposed fix

The test scenario has been modified in order to quickly and consistently reproduce the error discussed in the issue https://github.com/pgjdbc/pgjdbc/issues/1592.

A new driver file, postgresql-42.2.6-patched.jar has the following fix.
 
```
postgresql-42.2.6-patch.

--- a/pgjdbc/src/main/java/org/postgresql/core/VisibleBufferedInputStream.java
+++ b/pgjdbc/src/main/java/org/postgresql/core/VisibleBufferedInputStream.java
@@ -8,6 +8,7 @@ package org.postgresql.core;
 import java.io.EOFException;
 import java.io.IOException;
 import java.io.InputStream;
+import java.net.SocketTimeoutException;
 
 /**
  * A faster version of BufferedInputStream. Does no synchronisation and allows direct access to the
@@ -137,7 +138,12 @@ public class VisibleBufferedInputStream extends InputStream {
       }
       canFit = buffer.length - endIndex;
     }
-    int read = wrapped.read(buffer, endIndex, canFit);
+    int read = 0;
+    try {
+      read = wrapped.read(buffer, endIndex, canFit);
+    } catch (SocketTimeoutException e) {
+      // ignore
+    }
     if (read < 0) {
       return false;
     }
@@ -211,7 +217,12 @@ public class VisibleBufferedInputStream extends InputStream {
 
     // then directly from wrapped stream
     do {
-      int r = wrapped.read(to, off, len);
+      int r;
+      try {
+        r = wrapped.read(to, off, len);
+      } catch (SocketTimeoutException e) {
+        return read;
+      }
       if (r <= 0) {
         return (read == 0) ? r : read;
       }

```

**To test with the fix**

Uncomment the following lines in docker-compose.yml and run the test again.

```
    environment:
      PGJDBC_DRIVER: postgresql-42.2.6-patched.jar 
```
