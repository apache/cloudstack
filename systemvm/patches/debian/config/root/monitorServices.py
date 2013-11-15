#!/usr/bin/python
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




__author__ = 'jayapalreddy'

from ConfigParser import SafeConfigParser
from subprocess import *
from os import path
import time

monitor_log='/var/log/monitor.log'
class StatusCodes:
    SUCCESS      = 0
    FAILED       = 1
    INVALID_INP  = 2
    RUNNING      = 3
    STOPPED      = 4
    STARTING     = 5

class log:
    INFO = 'INFO'
    ALERT = 'ALERT'
    CRIT  = 'CRIT'
    NOTIF = 'NOTIF'




def getConfig( config_file_path = "/etc/monitor.conf" ):
    process_dict = {}
    parser = SafeConfigParser()
    parser.read( config_file_path )

    #print 'Read values:\n'

    for section in parser.sections():
        #   print section
        process_dict[section] = {}

        for name, value in parser.items(section):
            process_dict[section][name] = value
#           print '  %s = %r' % (name, value)

    return  process_dict

def printd (msg):

    return 0

    f= open(monitor_log,'r+')
    f.seek(0, 2)
    f.write(str(msg)+"\n")
    f.close()

def raisealert(severity, msg, process_name=None):
    #timeStr=str(time.ctime())
    if process_name is not None:
        log = '['+severity +']'+" " + '['+process_name+']' + " " + msg +"\n"
    else:
        log = '['+severity+']' + " " + msg +"\n"

    msg = 'logger -t monit '+ log
    pout = Popen(msg, shell=True, stdout=PIPE)


def isPidMatchPidFile(pidfile, pids):

    if pids is None or isinstance(pids,list) != True or len(pids) == 0:
        print "Invalid Arguments"
        return StatusCodes.FAILED
    if not path.isfile(pidfile):
        #It seems there is no pid file for this service
        printd("The pid file "+pidfile+" is not there for this process")
        return StatusCodes.FAILED

    fd=None
    try:
        fd = open(pidfile,'r')
    except:
        printd("pid file: "+ pidfile +" open failed")
        return StatusCodes.FAILED


    inp = fd.read()
    printd("file content "+str(inp))
    printd(pids)
    tocheck_pid  =  inp.strip()
    for item in pids:
        if str(tocheck_pid) ==  item.strip():
            printd("pid file matched")
            return StatusCodes.SUCCESS

    fd.close()
    return StatusCodes.FAILED



def checkProcessStatus( process ):
    process_name = process.get('processname')
    service_name = process.get('servicename')
    pidfile = process.get('pidfile')
    #temp_out = None
    restartFailed=False
    pidFileMatched=1
    cmd=''
    if process_name is None:
        print "\n Invalid Process Name"
        return StatusCodes.INVALID_INP
    else:
        msg="checking the process " + process_name
        printd(msg)
        cmd = 'pidof ' + process_name
        printd(cmd)
        #cmd = 'service ' + process_name + ' status'
        pout = Popen(cmd, shell=True, stdout=PIPE)
        exitStatus = pout.wait()
        temp_out = pout.communicate()[0]

    #check there is only one pid or not
    if exitStatus == 0:
        msg="pids: " +temp_out;
        printd(msg)
        pids = temp_out.split(' ')

        #there is more than one process so match the pid file
        #if not matched set pidFileMatched=0
        printd("Checking pid file")
        if isPidMatchPidFile(pidfile, pids) == StatusCodes.SUCCESS:
            pidFileMatched = 1;
        else:
            pidFileMatched = 0;

    printd(pidFileMatched)
    if exitStatus == 0 and pidFileMatched == 1:
        printd("The process is running ....")
        return  StatusCodes.RUNNING
    else:
        printd('exit status:'+str(exitStatus))
        msg="The process " + process_name +" is not running trying recover "
        printd(msg)
        #Retry the process state for few seconds
        for i in range(1,10):
            pout = Popen(cmd, shell=True, stdout=PIPE)
            exitStatus = pout.wait()
            temp_out = pout.communicate()[0]

            if i < 5: # this is just for trying few more times
                if exitStatus == 0:
                    pids = temp_out.split(' ')

                    if isPidMatchPidFile(pidfile, pids) == StatusCodes.SUCCESS:
                        pidFileMatched = 1;
                        printd("pid file is matched ...")
                        raisealert(log.ALERT, "The process detected as running", process_name)
                        break
                    else:
                        printd("pid file is not matched ...")
                        pidFileMatched = 0;
                        continue
                    time.sleep(1)
            else:
                msg="The process " +process_name+" is not running trying recover "
                raisealert(log.INFO,process_name,msg)

                if service_name == 'apache2':
                    # Killing apache2 process with this the main service will not start
                    for pid in pids:
                        cmd = 'kill -9 '+pid;
                        printd(cmd)
                        Popen(cmd, shell=True, stdout=PIPE, stderr=STDOUT)

                cmd = 'service ' + service_name + ' restart'

                time.sleep(1)
                #return_val= check_call(cmd , shell=True)

                cout = Popen(cmd, shell=True, stdout=PIPE, stderr=STDOUT)
                return_val = cout.wait()

                if return_val == 0:
                    printd("The process" + process_name +" recovered successfully ")
                    msg="The process " +process_name+" is recovered successfully "
                    raisealert(log.INFO,msg,process_name)

                    break;
                else:
                    #retry restarting the process for few tries
                    printd("process restart failing trying again ....")
                    restartFailed=True
                    time.sleep(1)
                    continue
        #for end here

        if restartFailed == True:
            msg="The process %s recover failed "%process_name
            raisealert(log.ALERT,process_name,msg)

            printd("Restart failed after number of retries")
            return StatusCodes.STOPPED

    return  StatusCodes.RUNNING

def raiseAlert( process_name ):
    print "process name %s is raised "%process_name

def monitProcess( processes_info ):
    if len( processes_info ) == 0:
        print "Invalid Input"
        return  StatusCodes.INVALID_INP
    for process,properties in processes_info.items():
        if checkProcessStatus( properties) != StatusCodes.RUNNING:
            print "\n Process %s is not Running"%process


def main():
    '''
    Step1 : Get Config
    '''

    printd("monitoring started")
    temp_dict  = getConfig()

    '''
    Step2: Get Previous Run Log
    '''

    '''
    Step3: Monitor and Raise Alert
    '''
    #raisealert(log.INFO, 'Monit started')
    monitProcess( temp_dict )


if __name__ == "__main__":
    main()







