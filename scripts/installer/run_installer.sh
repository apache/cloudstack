#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


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
