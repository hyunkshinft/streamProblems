#!/bin/bash -x
cd /data; [ ! -f break.csv ] && gunzip break.csv.gz;
while :
do
    psql -U test_user -h localhost -d test_database -c "begin; COPY test_rows(name) from '/data/break.csv'; commit;"
    sleep 30
done
