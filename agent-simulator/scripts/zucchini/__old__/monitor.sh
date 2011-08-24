#!/bin/bash
host=$1

while [ 1 ] 
do
	echo  ==== $(date) ==== 
#	mysql -uroot -Dcloud -h$host -e"select count(*), status, mgmt_server_id from host group by status, mgmt_server_id;"
	mysql -uroot -Dcloud -h$host -e"select count(*), state, type from vm_instance group by state, type;"
	mysql -uroot -Dcloud -h$host -e"select avg(timestampdiff(second,created,last_updated)),count(id),job_cmd,job_status,job_result_code from async_job where last_updated is not null group by job_cmd,job_status,job_result_code;"
	mysql -uroot -Dcloud -h$host -e "select count(*) as locks from op_lock;"
	echo === last 5 successful DeployVM ===
	mysql -uroot -Dcloud -h$host -e"select created,last_updated,id, timestampdiff(second,created,last_updated) from async_job where job_cmd like '%DeployVM%' and job_result_code=0 and last_updated is not null order by id desc limit 5;"
	echo === nwgroup status ===
	mysql -uroot -Dcloud -h$host -e"select step, count(*) from op_nwgrp_work group by step;"
	mysql -uroot -Dcloud -h$host -e"select avg(timestampdiff(second,created,taken)), count(id),mgmt_server_id from op_nwgrp_work where step='Done' and taken is not null and created < taken group by mgmt_server_id;"
	mysql -uroot -Dcloud -h$host -e"select id,mgmt_server_id,instance_id,created,taken,timestampdiff(second,created,taken) from op_nwgrp_work where taken is not null and created!=taken order by id desc limit 5;"
	echo 
	echo
	echo
	sleep 30s
done
