#!/bin/sh
# Imports the schema to create the content Database
######################################################

#mysql -h<hostname> -u<username> -p<password> < cbo_db_objects.sql
mysql -hlocalhost -uroot -proot < cbo_db_objects.sql
mysql -hlocalhost -uroot -proot < cbo_db_procedures.sql

