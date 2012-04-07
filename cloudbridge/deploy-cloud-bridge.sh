#!/usr/bin/env bash
# deploy.sh -- deploys a cloud-bridge
#

usage() {
  printf "Usage: %s: -d [tomcat directory to deploy to] -z [zip file to use]\n" $(basename $0) >&2
}

dflag=
zflag=
tflag=
iflag=

deploydir=
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
   printf "Tomcat directory was not specified, please set CATALINA_HOME environment variable'\n";
   exit 15;
fi

printf "Check to see if the Tomcat directory exist: $deploydir\n"
if [ ! -d $deploydir ]
then
    printf "Tomcat directory does not exist\n";
    exit 16;
fi

rm -rf $deploydir/webapps/bridge
mkdir "$CATALINA_HOME/temp"
mkdir "$CATALINA_HOME/webapps/bridge"


if ! unzip -o ./axis2.war -d $deploydir/webapps/bridge
then
   exit 10;
fi

if ! cp -f services/*  $deploydir/webapps/bridge/WEB-INF/services
then
   exit 11;
fi

if ! cp -f modules/*  $deploydir/webapps/bridge/WEB-INF/modules
then
   exit 12;
fi

if ! cp -f rampart-lib/*  $deploydir/webapps/bridge/WEB-INF/lib
then
   exit 13;
fi

if ! cp -f cloud-bridge.jar  $deploydir/webapps/bridge/WEB-INF/lib
then
   exit 14;
fi

if ! cp -f lib/*  $deploydir/lib
then
   exit 17;
fi

if ! cp -n conf/*  $deploydir/conf
then
   exit 18;
fi

if ! cp -f classes/*  $deploydir/webapps/bridge/WEB-INF/classes
then
   exit 19;
fi

if ! cp -f web.xml  $deploydir/webapps/bridge/WEB-INF
then
   exit 20;
fi

if ! cp -f axis2.xml  $deploydir/webapps/bridge/WEB-INF/conf
then
   exit 21;
fi

if ! rm -rf $deploydir/webapps/bridge/WEB-INF/lib/dom4j-1.6.1.jar
then
   exit 22;
fi


printf "Installation is now complete\n"
exit 0
