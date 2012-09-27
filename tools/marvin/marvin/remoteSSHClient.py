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

import paramiko 
import time
import cloudstackException

class remoteSSHClient(object):
    def __init__(self, host, port, user, passwd, retries = 10):
        self.host = host
        self.port = port
        self.user = user
        self.passwd = passwd
        self.ssh = paramiko.SSHClient()
        self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

        retry_count = retries
        while True:
            try:
                self.ssh.connect(str(host),int(port), user, passwd)
            except paramiko.SSHException, sshex:
                if retry_count == 0:
                    raise cloudstackException.InvalidParameterException(repr(sshex))
                retry_count = retry_count - 1
                time.sleep(5)
            except paramiko.AuthenticationException, authEx:
                raise cloudstackException.InvalidParameterException("Invalid credentials to login to %s on port %s"%(str(host), port))
            else:
                return
        
    def execute(self, command):
        stdin, stdout, stderr = self.ssh.exec_command(command)
        output = stdout.readlines()
        errors = stderr.readlines()
        results = []
        if output is not None and len(output) == 0:
            if errors is not None and len(errors) > 0:
                for error in errors:
                    results.append(error.rstrip())
            
        else:
            for strOut in output:
                results.append(strOut.rstrip())
    
        return results
    
    def scp(self, srcFile, destPath):
        transport = paramiko.Transport((self.host, int(self.port)))
        transport.connect(username = self.user, password=self.passwd)
        sftp = paramiko.SFTPClient.from_transport(transport)
        try:
            sftp.put(srcFile, destPath)
        except IOError, e:
            raise e

            
if __name__ == "__main__":
    ssh = remoteSSHClient("192.168.137.2", 22, "root", "password")
    print ssh.execute("ls -l")
    print ssh.execute("rm x")
