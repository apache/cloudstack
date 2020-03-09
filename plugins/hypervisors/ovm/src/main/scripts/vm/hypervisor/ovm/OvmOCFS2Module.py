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
from OvmCommonModule import *

logger = OvmLogger('OvmOCFS2')  
class OvmOCFS2(OvmObject):
    def _prepareConf(self, cluster):
        conf = '''cluster:
        node_count = 0
        name = %s
        '''%cluster
        dir = dirname(OCFS2_CONF)
        if not isdir(dir):
            os.makedirs(dir)
            
        fd = open(OCFS2_CONF, 'w')
        fd.write(conf)
        fd.close()
        
    def _addNode(self, name, nodeNum, ip, port, cluster, isOnline=True):
        nodePath = '/sys/kernel/config/cluster/%s/node/%s'%(cluster, name)
        if exists(nodePath):
            logger.debug(OvmOCFS2._addNode, "node %s already exists, skip it(%s)"%(name, nodePath))
            return
        
        if not isOnline:
            cmds = ['o2cb_ctl -C -n', name, '-t node', '-a number=%s'%nodeNum, '-a ip_address=%s'%ip, '-a ip_port=%s'%port, '-a cluster=%s'%cluster]
        else:
            cmds = ['o2cb_ctl -C -i -n', name, '-t node', '-a number=%s'%nodeNum, '-a ip_address=%s'%ip, '-a ip_port=%s'%port, '-a cluster=%s'%cluster]
        
        try:
            doCmd(cmds)
        except ShellExceutedFailedException, e:
            if e.errCode == 239 or "already exists" in e.stderr:
                logger.debug(OvmOCFS2._addNode, "node %s already exists, skip it(%s)"%(name, e.stderr))
            else:
                raise e
    
    def _isClusterOnline(self, cluster):
        cmds = ['service o2cb status', cluster]
        res = doCmd(cmds)
        for line in res.split('\n'):
            if not 'Checking O2CB cluster' in line: continue
            return not 'Offline' in line
    
    def _load(self):
        cmd = ['service o2cb load']
        doCmd(cmd)
        
    def _start(self, cluster):
        #blank line are answer by clicking enter
        config='''
y
o2cb
%s




EOF
'''%cluster
        cmd = ['service o2cb configure', '<<EOF', config]
        doCmd(cmd)
        cmd = ['service o2cb start %s'%cluster]
        doCmd(cmd)
                
