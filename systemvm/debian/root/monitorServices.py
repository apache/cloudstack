#!/usr/bin/python3
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

from configparser import SafeConfigParser
from subprocess import *
from datetime import datetime
import time
import os
import logging
import json
from os import sys, path
from health_checks.utility import getHealthChecksData

class StatusCodes:
    SUCCESS      = 0
    FAILED       = 1
    INVALID_INP  = 2
    RUNNING      = 3
    STOPPED      = 4
    STARTING     = 5

class Log:
    INFO = 'INFO'
    ALERT = 'ALERT'
    CRIT  = 'CRIT'
    NOTIF = 'NOTIF'

class Config:
    SLEEP_SEC = 1
    RETRY_ITERATIONS = 10
    RETRY_FOR_RESTART = 5
    MONITOR_LOG = '/var/log/monitor.log'
    HEALTH_CHECKS_DIR = 'health_checks'
    MONITOR_RESULT_FILE_SUFFIX = 'monitor_results.json'
    FAILING_CHECKS_FILE = 'failing_health_checks'

def getServicesConfig( config_file_path = "/etc/monitor.conf" ):
    """
    Reads the process configuration from the config file.
    Config file contains the processes to be monitored.

    """
    process_dict = {}
    parser = SafeConfigParser()
    parser.read( config_file_path )


    for section in parser.sections():
        process_dict[section] = {}

        for name, value in parser.items(section):
            process_dict[section][name] = value
            printd (" %s = %r" % (name, value))

    return  process_dict

def printd (msg):
    """
    prints the debug messages
    """

    #for debug
    #print msg

    f= open(Config.MONITOR_LOG, 'w' if not path.isfile(Config.MONITOR_LOG) else 'r+')
    f.seek(0, 2)
    f.write(str(msg)+"\n")
    f.close()
    print(str(msg))

def raisealert(severity, msg, process_name=None):
    """ Writes the alert message"""

    #timeStr=str(time.ctime())
    if process_name is not None:
        log = '['+severity +']'+" " + '['+process_name+']' + " " + msg +"\n"
    else:
        log = '['+severity+']' + " " + msg +"\n"

    logging.basicConfig(level=logging.INFO,filename='/var/log/routerServiceMonitor.log',format='%(asctime)s %(message)s')
    logging.info(log)
    msg = 'logger -t monit '+ log
    pout = Popen(msg, shell=True, stdout=PIPE)
    print("[Alert] " + msg)


def isPidMatchPidFile(pidfile, pids):
    """ Compares the running process pid with the pid in pid file.
        If a process with multiple pids then it matches with pid file
    """

    if pids is None or isinstance(pids,list) != True or len(pids) == 0:
        printd ("Invalid Arguments")
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

    if not inp:
        fd.close()
        return StatusCodes.FAILED

    printd("file content of pidfile " + pidfile + " = " + str(inp).strip())
    printd(pids)
    tocheck_pid  =  inp.strip()
    for item in pids:
        if str(tocheck_pid) ==  item.strip():
            printd("pid file matched")
            fd.close()
            return StatusCodes.SUCCESS

    fd.close()
    return StatusCodes.FAILED

def checkProcessRunningStatus(process_name, pidFile):
    printd("checking the process " + process_name)
    cmd = ''
    pids = []
    cmd = 'pidof ' + process_name
    printd(cmd)

    #cmd = 'service ' + process_name + ' status'
    pout = Popen(cmd, shell=True, stdout=PIPE)
    exitStatus = pout.wait()
    temp_out = pout.communicate()[0]

    #check there is only one pid or not
    if exitStatus == 0:
        pids = temp_out.strip().split(' ')
        printd("pid(s) of process %s are %s " %(process_name, pids))

        #there is more than one process so match the pid file
        #if not matched set pidFileMatched=False
        printd("Checking pid file")
        if isPidMatchPidFile(pidFile, pids) == StatusCodes.SUCCESS:
            return True,pids

    printd("pid of exit status %s" %exitStatus)

    return False,pids

def restartService(service_name):

    cmd = 'service ' + service_name + ' restart'
    cout = Popen(cmd, shell=True, stdout=PIPE, stderr=STDOUT)
    return_val = cout.wait()

    if return_val == 0:
        printd("The service " + service_name +" recovered successfully ")
        msg="The process " +service_name+" is recovered successfully "
        raisealert(Log.INFO,msg,service_name)
        return True
    else:
        printd("process restart failed ....")

    return False

def checkProcessStatus( process ):
    """
    Check the process running status, if not running tries to restart
    Returns the process status and if it was restarted
    """
    process_name = process.get('processname')
    service_name = process.get('servicename')
    pidfile = process.get('pidfile')
    #temp_out = None
    restartFailed=False
    pidFileMatched=False
    pids=''
    cmd=''
    if process_name is None:
        printd ("\n Invalid Process Name")
        return StatusCodes.INVALID_INP, False

    status, pids = checkProcessRunningStatus(process_name, pidfile)

    if status == True:
        printd("The process is running ....")
        return StatusCodes.RUNNING, False
    else:
        printd("Process %s is not running trying to recover" %process_name)
        #Retry the process state for few seconds

        for i in range(1, Config.RETRY_ITERATIONS):
            time.sleep(Config.SLEEP_SEC)

            if i < Config.RETRY_FOR_RESTART: # this is just for trying few more times

                status, pids = checkProcessRunningStatus(process_name, pidfile)
                if status == True:
                    raisealert(Log.ALERT, "The process detected as running", process_name)
                    break
                else:
                    printd("Process %s is not running checking the status again..." %process_name)
                    continue
            else:
                msg="The process " +process_name+" is not running trying recover "
                raisealert(Log.INFO,process_name,msg)

                if service_name == 'apache2':
                    # Killing apache2 process with this the main service will not start
                    for pid in pids:
                        cmd = 'kill -9 '+pid
                        printd(cmd)
                        Popen(cmd, shell=True, stdout=PIPE, stderr=STDOUT)

                if restartService(service_name) == True:
                    break
                else:
                    restartFailed = True
                    continue
        #for end here

        if restartFailed == True:
            msg="The process %s recover failed "%process_name
            raisealert(Log.ALERT,process_name,msg)

            printd("Restart failed after number of retries")
            return StatusCodes.STOPPED, False

        return StatusCodes.RUNNING, True


def monitProcess( processes_info ):
    """
    Monitors the processes which got from the config file
    """
    checkStartTime = time.time()
    service_status = {}
    failing_services = []
    if len( processes_info ) == 0:
        printd("No config items provided - means a redundant VR or a VPC Router")
        return service_status, failing_services

    print("[Process Info] " + json.dumps(processes_info))

    #time for noting process down time
    csec = repr(time.time()).split('.')[0]

    for process,properties in list(processes_info.items()):
        printd ("---------------------------\nchecking the service %s\n---------------------------- " %process)
        serviceName = process + ".service"
        processStatus, wasRestarted = checkProcessStatus(properties)
        if processStatus != StatusCodes.RUNNING:
            printd( "\n Service %s is not Running"%process)
            checkEndTime = time.time()
            service_status[serviceName] = {
                "success": "false",
                "lastUpdate": str(int(checkStartTime * 1000)),
                "lastRunDuration": str((checkEndTime - checkStartTime) * 1000),
                "message": "service down at last check " + str(csec)
            }
            failing_services.append(serviceName)
        else:
            checkEndTime = time.time()
            service_status[serviceName] = {
                "success": "true",
                "lastUpdate": str(int(checkStartTime * 1000)),
                "lastRunDuration": str((checkEndTime - checkStartTime) * 1000),
                "message": "service is running" + (", was restarted" if wasRestarted else "")
            }

    return service_status, failing_services


def execute(script, checkType = "basic"):
    checkStartTime = time.time()
    cmd = "./" + script + " " + checkType
    printd ("Executing health check script command: " + cmd)

    pout = Popen(cmd, shell=True, stdout=PIPE)
    exitStatus = pout.wait()
    output = pout.communicate()[0].strip()
    checkEndTime = time.time()

    if exitStatus == 0:
        if len(output) > 0:
            printd("Successful execution of " + script)
            return {
                "success": "true",
                "lastUpdate": str(int(checkStartTime * 1000)),
                "lastRunDuration": str((checkEndTime - checkStartTime) * 1000),
                "message": output
            }
        return {} #Skip script if no output is received
    else:
        printd("Script execution failed " + script)
        return {
            "success": "false",
            "lastUpdate": str(int(checkStartTime * 1000)),
            "lastRunDuration": str((checkEndTime - checkStartTime) * 1000),
            "message": output
        }

def main(checkType = "basic"):
    startTime = time.time()
    '''
    Step1 : Get Services Config
    '''
    printd("monitoring started")
    configDict = getServicesConfig()

    '''
    Step2: Monitor services and Raise Alerts
    '''
    monitResult = {}
    failingChecks = []
    if checkType == "basic":
        monitResult, failingChecks = monitProcess(configDict)

    '''
    Step3: Run health check scripts as needed
    '''
    hc_data = getHealthChecksData()

    if hc_data is not None and "health_checks_enabled" in hc_data and hc_data['health_checks_enabled']:
        hc_exclude = hc_data["excluded_health_checks"] if "excluded_health_checks" in hc_data else []
        for f in os.listdir(Config.HEALTH_CHECKS_DIR):
            if f in hc_exclude:
                continue
            fpath = path.join(Config.HEALTH_CHECKS_DIR, f)
            if path.isfile(fpath) and os.access(fpath, os.X_OK):
                ret = execute(fpath, checkType)
                if len(ret) == 0:
                    continue
                if "success" in ret and ret["success"].lower() == "false":
                    failingChecks.append(f)
                monitResult[f] = ret

    '''
    Step4: Write results to the json file for admins/management server to read
    '''

    endTime = time.time()
    monitResult["lastRun"] = {
        "start": str(datetime.fromtimestamp(startTime)),
        "end": str(datetime.fromtimestamp(endTime)),
        "duration": str(endTime - startTime)
    }

    with open(checkType + "_" + Config.MONITOR_RESULT_FILE_SUFFIX, 'w') as f:
        json.dump(monitResult, f, ensure_ascii=False)

    failChecksFile = checkType + "_" + Config.FAILING_CHECKS_FILE
    if len(failingChecks) > 0:
        fcs = ""
        for fc in failingChecks:
            fcs = fcs + fc + ","
        fcs = fcs[0:-1]
        with open(failChecksFile, 'w') as f:
            f.write(fcs)
    elif path.isfile(failChecksFile):
        os.remove(failChecksFile)

if __name__ == "__main__":
    checkType = "basic"
    if len(sys.argv) == 2:
        if sys.argv[1] == "advanced":
            main("advanced")
        elif sys.argv[1] == "basic":
            main("basic")
        else:
            printd("Error: Unknown type of test: " + sys.argv)
    else:
        main("basic")
        main("advanced")
