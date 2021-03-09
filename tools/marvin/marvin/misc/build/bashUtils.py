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


from optparse import OptionParser
from signal import alarm, signal, SIGALRM, SIGKILL
from subprocess import PIPE, Popen
import logging
import os
import paramiko
import select


class wget(object):
    def __init__(self, filename, url, path=None):
        pass
    
    def background(self, handler):
        pass

class remoteSSHClient(object):
    def __init__(self, host, port, user, passwd):
        self.host = host
        self.port = port
        self.user = user
        self.passwd = passwd
        self.ssh = paramiko.SSHClient()
        self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            self.ssh.connect(str(host),int(port), user, passwd)
        except paramiko.SSHException as sshex:
            logging.debug(repr(sshex))
        
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
    
    def execute_buffered(self, command, bufsize=512):
        transport = self.ssh.get_transport()
        channel = transport.open_session()
        try:
            stdin, stdout, sterr = channel.exec_command(command)
            while True:
                rl, wl, xl = select.select([channel],[],[],0.0)
                if len(rl) > 0:
                  logging.debug(channel.recv(bufsize))
        except paramiko.SSHException as e:
            logging.debug(repr(e))    

            
    def scp(self, srcFile, destPath):
        transport = paramiko.Transport((self.host, int(self.port)))
        transport.connect(username = self.user, password=self.passwd)
        sftp = paramiko.SFTPClient.from_transport(transport)
        try:
            sftp.put(srcFile, destPath)
        except IOError as e:
            raise e

class bash:
    def __init__(self, args, timeout=600):
        self.args = args
        logging.debug("execute:%s"%args)
        self.timeout = timeout
        self.process = None
        self.success = False
        self.run()

    def run(self):
        class Alarm(Exception):
            pass
        def alarm_handler(signum, frame):
            raise Alarm

        try:
            self.process = Popen(self.args, shell=True, stdout=PIPE, stderr=PIPE)
            if self.timeout != -1:
                signal(SIGALRM, alarm_handler)
                alarm(self.timeout)

            try:
                self.stdout, self.stderr = self.process.communicate()
                if self.timeout != -1:
                    alarm(0)
            except Alarm:
                os.kill(self.process.pid, SIGKILL)
                

            self.success = self.process.returncode == 0
        except:
            pass

        if not self.success: 
            logging.debug("Failed to execute:" + self.getErrMsg())

    def isSuccess(self):
        return self.success
    
    def getStdout(self):
        try:
            return self.stdout.strip("\n")
        except AttributeError:
            return ""
    
    def getLines(self):
        return self.stdout.split("\n")

    def getStderr(self):
        try:
            return self.stderr.strip("\n")
        except AttributeError:
            return ""
    
    def getErrMsg(self):
        if self.isSuccess():
            return ""
        
        if self.getStderr() is None or self.getStderr() == "":
            return self.getStdout()
        else:
            return self.getStderr()

