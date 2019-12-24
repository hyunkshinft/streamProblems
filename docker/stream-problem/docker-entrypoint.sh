#! /bin/sh

POSTGRES_USER="test_user"
POSTGRES_DB="test_database"

wait_for_postgres() {
  echo -n 'Waiting for postgres.'
  until psql -h postgres -U $POSTGRES_USER $POSTGRES_DB -c 'SELECT 1;' &>/dev/null
  do
    echo -n '.'
    sleep 1
  done
  echo 'done'
}

wait_for_postgres
psql -h postgres -U $POSTGRES_USER $POSTGRES_DB -c "select pg_drop_replication_slot('test_slot');" || true
psql -h postgres -U $POSTGRES_USER $POSTGRES_DB -c "SELECT * FROM pg_create_logical_replication_slot('test_slot', 'test_decoding');"

[ -n "$PGJDBC_DRIVER" ] && {
 echo Replacing the PGJDBC dirver with /streamProblems/${PGJDBC_DRIVER}
  mvn install:install-file -Dfile=/streamProblems/${PGJDBC_DRIVER}  \
    -DgroupId=org.postgresql \
    -DartifactId=postgresql \
    -Dversion=42.2.9 \
    -Dpackaging=jar \
    -DgeneratePom=true
}

cd /streamProblems && mvn clean && mvn package

java -cp /streamProblems/docker/stream-problem/streamProblems-1.0.jar streamProblem
