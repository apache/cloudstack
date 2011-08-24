#/bin/bash



  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
 

name=$1
while [ 1 ]
do
	mysql -s -r -uroot -Dcloud -h10.1.1.215 -e"select count(id),now(),max(disconnected),mgmt_server_id,status from host group by mgmt_server_id,status;" >> $1
	sleep 5
	echo --------------------------------------------------------------------------------------------------------------------- >> $1
done
