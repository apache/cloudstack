rem - Licensed to the Apache Software Foundation (ASF) under one
rem - or more contributor license agreements.  See the NOTICE file
rem - distributed with this work for additional information
rem - regarding copyright ownership.  The ASF licenses this file
rem - to you under the Apache License, Version 2.0 (the
rem - "License"); you may not use this file except in compliance
rem - with the License.  You may obtain a copy of the License at
rem - 
rem -   http://www.apache.org/licenses/LICENSE-2.0
rem - 
rem - Unless required by applicable law or agreed to in writing,
rem - software distributed under the License is distributed on an
rem - "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem - KIND, either express or implied.  See the License for the
rem - specific language governing permissions and limitations
rem - under the License.

java -cp cloud-utils.jar;agent-simulator.jar;log4j-1.2.15.jar;apache-log4j-extras-1.0.jar;ws-commons-util-1.0.2.jar;xmlrpc-client-3.1.3.jar;cloud-agent.jar;cloud-core.jar;xmlrpc-common-3.1.3.jar;javaee-api-5.0-1.jar;gson-1.3.jar;commons-httpclient-3.1.jar;commons-logging-1.1.1.jar;commons-codec-1.3.jar;commons-collections-3.2.1.jar;commons-pool-1.4.jar;.\;.\conf com.cloud.agent.AgentSimulator %*

