#!/bin/bash -e

function createLogFile(){
	if [ ! -f ${LOGDIR}/cbodatacleanup-$(date +"%Y-%m-%d").log ] 
	then 
	touch ${LOGDIR}/cbodatacleanup-$(date +"%Y-%m-%d").log 
	fi	
}


function getProperty(){
	propertyName=$1
	propertyValue=`sed '/^\#/d' /opt/Tumri/cbo/current/tomcat7/conf/cbo.properties | grep "$propertyName"  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
	echo "$propertyValue"
}

function writeToLog(){
	log_message_string=$( echo "$1"|sed -e 's/^ *//g' -e 's/ *$//g' )
	#IFS - Inter Field Separator - Capture the current IFS in a variable
	OLDIFS=$IFS
	IFS='|'
	for i in $log_message_string;do
		if [ ! -z "$i" ]
		then
		echo $(date +"%Y-%m-%d %T : ") $i >> ${LOGDIR}/cbodatacleanup-$(date +"%Y-%m-%d").log;
		fi
	done
	IFS=$OLDIFS
}

LOGDIR=$( getProperty com.tumri.cbo.log.dir )
echo "Log Directory : $LOGDIR"
createLogFile
db_data_retain_days=$( getProperty cbo_db.db_data_retain_days )
log_file_retain_days=$( getProperty logs.log_file_retain_days )
#check if $db_data_retain_days is null
if [ -z "$db_data_retain_days" ] 
then
	writeToLog "The parameter to delete databasse data older than x number of days is not set up in cbo.properties. Exiting the program"
	exit
fi
#check if $log_file_retain_days is null
if [ -z "$log_file_retain_days" ] 
then
	writeToLog "The parameter to delete log files older than x number of days is not set up in cbo.properties. Exiting the program"
	exit
fi	
db_host=$( getProperty cbo_db.host )
echo "db_host : $db_host"
db_port=$( getProperty cbo_db.port )
echo "db_port : $db_port"
db_username=$( getProperty cbo_db.username )
db_password=$( getProperty cbo_db.password )
db_name="cbo_db"
echo "db_name : $db_name"

writeToLog "Cleaning up campaign data which is older than $db_data_retain_days days"

mysqlcmd="mysql --host=$db_host --port=$db_port --user=$db_username --password=$db_password --database=$db_name --skip-column-names"

#clean up CHANGELOG table
output=$($mysqlcmd -e "CALL cbo_purge_data($db_data_retain_days, @error_code, @error_msg); SELECT @error_msg;")
writeToLog "$output"
if [ $? -ne 0 ]
then
	writeToLog "Error occurred during database clean-up of CBO_DB..."
else
	if [ `echo $output | grep -ic "error" ` -gt 0 ]
	then
		writeToLog "Error occurred during database clean-up of CBO_DB..."
	else
		writeToLog "Database clean-up of CBO_DB successful..."
	fi
fi
#cleaning up log files
cd /opt/Tumri/cbo/logs
writeToLog "Deleting CBO log files under /opt/Tumri/cbo/logs directory..."
find . -type f -mtime +$log_file_retain_days -exec basename {} \;|xargs -I{} rm -v {}
cd /opt/Tumri/cbo/current/tomcat7/logs
writeToLog "Deleting tomcat log files under /opt/Tumri/cbo/current/tomcat7/logs..."
find . -type f -mtime +$log_file_retain_days -exec basename {} \;|xargs -I{} rm -v {}
writeToLog "Log file clean up of CBO application successful"
writeToLog "Exiting the program..."
echo "Data clean up task completed..."
exit
