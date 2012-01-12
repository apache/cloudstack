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



 

# $Id: run_installer.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/run_installer.sh $

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
