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



 

#run.sh runs the agent client.
java $1 -Xms128M -Xmx384M -cp cglib-nodep-2.2.jar:trilead-ssh2-build213.jar:cloud-api.jar:cloud-core-extras.jar:cloud-utils.jar:cloud-agent.jar:cloud-console-proxy.jar:cloud-console-common.jar:freemarker.jar:log4j-1.2.15.jar:ws-commons-util-1.0.2.jar:xmlrpc-client-3.1.3.jar:cloud-core.jar:xmlrpc-common-3.1.3.jar:javaee-api-5.0-1.jar:gson-1.3.jar:commons-httpclient-3.1.jar:commons-logging-1.1.1.jar:commons-codec-1.4.jar:commons-collections-3.2.1.jar:commons-pool-1.4.jar:apache-log4j-extras-1.0.jar:libvirt-0.4.5.jar:jna.jar:.:/etc/cloud:./conf com.cloud.agent.AgentShell
