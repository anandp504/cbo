#!/bin/sh
# Drop content database
######################################################

#mysql -h<hostname> -u<username> -p<password> -e <statement to be executed>
mysql -hlocalhost -uroot -proot -e 'DROP DATABASE cbo_db';

