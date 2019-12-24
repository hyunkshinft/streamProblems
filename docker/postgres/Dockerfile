FROM postgres:11.4

ADD pg_hba.conf /etc/postgresql/pg_hba.conf
ADD postgresql.conf /etc/postgresql/postgresql.conf
ADD create_tables.sql /docker-entrypoint-initdb.d/create_tables.sql

ADD login_db.sh /
ADD pump_data.sh /

CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf", "-c", "hba_file=/etc/postgresql/pg_hba.conf"]
