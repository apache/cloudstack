#!/bin/bash
#
# update database connection
# start cloudstack-management server
#/usr/bin/cloudstack-setup-databases cloud:password@$MYSQL_PORT_3306_TCP_ADDR

# initial startup of the container to generage ssh_key
# performed as privileged
if [ ! -d /var/cloudstack/management/.ssh ]; then
	mknod /dev/loop4 -m0660 b 7 4
fi
#
service mysqld start
service cloudstack-management start
sleep 20 # wait for the file management-server.log  to be created
tail -f /var/log/cloudstack/management/management-server.log
