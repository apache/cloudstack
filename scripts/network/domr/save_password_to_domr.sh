#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

# $Id: save_password_to_domr.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/save_password_to_domr.sh $
# @VERSION@

PASSWD_FILE=/var/cache/cloud/passwords

#   $1 filename
#   $2 keyname
#   $3 value
replace_in_file_on_domr() {
  local filename=$1
  local keyname=$2
  local value=$3
  $VIA_SSH "sed -i /$keyname/d $filename; \
  		 	echo "$keyname=$value" >> $filename "
  		 	
  # $VIA_SSH "sed -e /$keyname/d $filename > $filename.new; \
  #        mv $filename.new $filename;\
  #         echo "$keyname=$value" >> $filename "
  
  return $?
}

cert="/root/.ssh/id_rsa.cloud"

while getopts 'r:v:p:' OPTION
do
  case $OPTION in
  r)	
		DOMR_IP="$OPTARG"
		;;
  v)	VM_IP="$OPTARG"
		;;
  p)	
		ENCODEDPASSWORD="$OPTARG"
		PASSWORD=$(echo $ENCODEDPASSWORD | tr '[a-m][n-z][A-M][N-Z]' '[n-z][a-m][N-Z][A-M]')
		;;
  ?)	echo "Incorrect usage"
		exit 1
		;;
  esac
done

VIA_SSH="ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$DOMR_IP"

$VIA_SSH "if [ ! -f $PASSWD_FILE ]; then touch $PASSWD_FILE; fi;"

replace_in_file_on_domr $PASSWD_FILE $VM_IP $PASSWORD

if [ $? -ne 0 ]
then
	exit 1
fi

exit 0
