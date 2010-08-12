#!/usr/bin/env bash
# $Id: run_installer.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/installer/run_installer.sh $

installer=$1
classname="com.vmops.installer.VMOpsSimpleSetup"

if [ "$installer" != "routing" ]
then
        if [ "$installer" != "storage" ]
        then
                echo "Valid installers: routing/storage"
                exit 1
        fi
fi

if [ "$VMOPS_HOME" == "" ]
then 
	VMOPS_HOME="/usr/local/vmops"
fi

export LD_LIBRARY_PATH=$(pwd)/lib:$LD_LIBRARY_PATH
java -cp $VMOPS_HOME/agent/log4j-1.2.15.jar:$VMOPS_HOME/agent/apache-log4j-extras-1.0.jar:$VMOPS_HOME/agent/commons-logging-1.1.1.jar:$VMOPS_HOME/agent/charva.jar:$VMOPS_HOME/agent/vmops-utils.jar:$VMOPS_HOME/agent/vmops-installer.jar:$VMOPS_HOME/agent/conf $classname $*
