#! /bin/sh

set -o nounset
set -o errexit

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
psql -h postgres -U $POSTGRES_USER $POSTGRES_DB -c "SELECT * FROM pg_create_logical_replication_slot('test_slot', 'test_decoding');"

java -cp streamProblems-1.0.jar streamProblem
