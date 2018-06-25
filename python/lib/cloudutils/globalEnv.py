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
class globalEnv:
    def __init__(self):
        #Agent/Server/Db
        self.mode = None
        #server mode: normal/mycloud
        self.svrMode = None
        #noStart: do not start mgmt server after configuration?
        self.noStart = False
        #myCloud/Agent/Console
        self.agentMode = None
        #debug
        self.debug = False
        #management server IP
        self.mgtSvr = "myagent.cloud.com"
        #zone id or zone name
        self.zone = None
        #pod id or pod name
        self.pod = None
        #cluster id or cluster name
        self.cluster = None
        #hypervisor type. KVM/LXC. Default is KVM
        self.hypervisor = "kvm"
        #nics: 0: private nic, 1: guest nic, 2: public nic used by agent
        self.nics = []
        #uuid
        self.uuid = None
        #default private network
        self.privateNet = "cloudbr0"
        #distribution
        self.distribution = None
        # bridgeType
        self.bridgeType = "native"
