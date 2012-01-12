#!/usr/bin/env bash
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



 

# deploy.sh -- deploys a management server

usage() {
  printf "Usage: %s: -d [tomcat directory to deploy to] -z [zip file to use]\n" $(basename $0) >&2
}

dflag=
zflag=
tflag=
iflag=

deploydir=
zipfile="client.zip"
typ=

#set -x

while getopts 'd:z:x:h:' OPTION
do
  case "$OPTION" in
  d)	dflag=1
	deploydir="$OPTARG"
		;;
  z)    zflag=1
        zipfile="$OPTARG"
                ;;
  h)    iflag="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$deploydir" == "" ]
then 
    if [ "$CATALINA_HOME" == "" ]
    then
        printf "Tomcat Directory to deploy to: "
        read deploydir
    else
        deploydir="$CATALINA_HOME"
    fi
fi

if [ "$deploydir" == "" ]
then 
   printf "Tomcat directory was not specified\n";
   exit 15;
fi

printf "Check to see if the Tomcat directory exist: $deploydir\n"
if [ ! -d $deploydir ]
then
    printf "Tomcat directory does not exist\n";
    exit 16;
fi

if [ "$zipfile" == "" ]
then 
    printf "Path of the zip file [defaults to client.zip]: "
    read zipfile
    if [ "$zipfile" == "" ]
    then
        zipfile="client.zip"
    fi
fi
if ! unzip -o $zipfile client.war 
then
  exit 6
fi

rm -fr $deploydir/webapps/client

if ! unzip -o ./client.war -d $deploydir/webapps/client
then
   exit 10;
fi

rm -f ./client.war

if ! unzip -o $zipfile lib/* -d $deploydir
then
   exit 11;
fi

if ! unzip -o $zipfile conf/* -d $deploydir
then
    exit 12;
fi

if ! unzip -o $zipfile bin/* -d $deploydir
then
    exit 13;
fi

printf "Adding the conf directory to the class loader for tomcat\n"
sed 's/shared.loader=$/shared.loader=\$\{catalina.home\},\$\{catalina.home\}\/conf\
/' $deploydir/conf/catalina.properties > $deploydir/conf/catalina.properties.tmp
mv $deploydir/conf/catalina.properties.tmp $deploydir/conf/catalina.properties

printf "Installation is now complete\n"
exit 0
