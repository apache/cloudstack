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

from ConfigParser import SafeConfigParser
from subprocess import *
from datetime import datetime
import time
import os
import logging
import json
from os import sys, path

sys.path.append('/opt/cloud/bin')
from healthchecksutility import getHealthChecksData

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
    MONIT_AFTER_MINS = 30
    SLEEP_SEC = 1
    RETRY_ITERATIONS = 10
    RETRY_FOR_RESTART = 5
    MONITOR_LOG = '/var/log/monitor.log'
    UNMONIT_PS_FILE = '/etc/unmonit_psList.txt'
    HEALTH_CHECKS_SCRIPTS_DIR = 'health_scripts'
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
    print str(msg)

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
    print "[Alert] " + msg


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
        return StatusCodes.INVALID_INP

    status, pids = checkProcessRunningStatus(process_name, pidfile)

    if status == True:
        printd("The process is running ....")
        return StatusCodes.RUNNING
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
            return StatusCodes.STOPPED

    return StatusCodes.RUNNING


def monitProcess( processes_info ):
    """
    Monitors the processes which got from the config file
    """
    service_status = {}
    failing_services = []
    if len( processes_info ) == 0:
        printd("No config items provided - means a redundant VR or a VPC Router")
        return service_status, failing_services

    print "[Process Info] " + json.dumps(processes_info)

    dict_unmonit={}
    umonit_update={}
    unMonitPs=False

    if not path.isfile(Config.UNMONIT_PS_FILE):
        printd('Unmonit File not exist')
    else:
        #load the dictionary with unmonit process list
        dict_unmonit = loadPsFromUnMonitFile()

    #time for noting process down time
    csec = repr(time.time()).split('.')[0]

    for process,properties in processes_info.items():
        serviceName = process + ".service"
        #skip the process it its time stamp less than Config.MONIT_AFTER_MINS
        printd ("---------------------------\nchecking the service %s\n---------------------------- " %process)
        if not is_emtpy(dict_unmonit):
            if dict_unmonit.has_key(process):
                ts = dict_unmonit[process]

                if checkPsTimeStampForMonitor (csec, ts, properties) == False:
                    service_status[serviceName] = {"success": "False", "message": "down since" + str(ts)}
                    failing_services.append(serviceName)
                    unMonitPs = True
                    continue

        if checkProcessStatus(properties) != StatusCodes.RUNNING:
            printd( "\n Service %s is not Running"%process)
            #add this process into unmonit list
            printd ("updating the service for unmonit %s\n" %process)
            umonit_update[process]=csec
            service_status[serviceName] = {"success": "False", "message": "down since" + str(csec)}
            failing_services.append(serviceName)
        else:
            service_status[serviceName] = {"success": "True", "message": "service is running"}

    #if dict is not empty write to file else delete it
    if not is_emtpy(umonit_update):
        writePsListToUnmonitFile(umonit_update)
    else:
        if is_emtpy(umonit_update) and unMonitPs == False:
            #delete file it is there
            removeFile(Config.UNMONIT_PS_FILE)
    return service_status, failing_services


def checkPsTimeStampForMonitor(csec,ts, process):
    printd("Time difference=%s" %str(int(csec) - int(ts)))
    tmin = (int(csec) - int(ts) )/60

    if ( int(csec) - int(ts) )/60 < Config.MONIT_AFTER_MINS:
        raisealert(Log.ALERT, "The %s get monitor after %s minutes " %(process, Config.MONIT_AFTER_MINS))
        printd('process will be monitored after %s min' %(str(int(Config.MONIT_AFTER_MINS) - tmin)))
        return False

    return  True

def removeFile(fileName):
    if path.isfile(fileName):
        printd("Removing the file %s" %fileName)
        os.remove(fileName)

def loadPsFromUnMonitFile():

    dict_unmonit = {}

    try:
        fd = open(Config.UNMONIT_PS_FILE)
    except:
        printd("Failed to open file %s " %(Config.UNMONIT_PS_FILE))
        return StatusCodes.FAILED

    ps = fd.read()

    if not ps:
        printd("File %s content is empty " %Config.UNMONIT_PS_FILE)
        return StatusCodes.FAILED

    printd(ps)
    plist = ps.split(',')
    plist.remove('')
    for i in plist:
        dict_unmonit[i.split(':')[0]] = i.split(':')[1]

    fd.close()

    return dict_unmonit


def writePsListToUnmonitFile(umonit_update):
    printd("Write updated unmonit list to file")
    line=''
    for i in umonit_update:
        line+=str(i)+":"+str(umonit_update[i])+','
    printd(line)
    try:
        fd=open(Config.UNMONIT_PS_FILE,'w')
    except:
        printd("Failed to open file %s " %Config.UNMONIT_PS_FILE)
        return StatusCodes.FAILED

    fd.write(line)
    fd.close()


def is_emtpy(struct):
    """
    Checks wether the given struct is empty or not
    """
    if struct:
        return False
    else:
        return True

def execute(script, checkType = "basic"):
    cmd = "./" + script + " " + checkType
    printd ("Executing health check script command: " + cmd)

    pout = Popen(cmd, shell=True, stdout=PIPE)
    exitStatus = pout.wait()
    output = pout.communicate()[0].strip()

    if exitStatus == 0:
        if len(output) > 0:
            printd("Successful execution of " + script)
            return {"success": "True", "message": output}
        return {} #Skip script if no output is received
    else:
        printd("Script execution failed " + script)
        return {"success": "False", "message": output}

def main(checkType = "basic"):
    startTime = int(time.time())
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

    if "health_checks_enabled" in hc_data and hc_data['health_checks_enabled']:
        hc_exclude = hc_data["excluded_health_checks"] if "excluded_health_checks" in hc_data else []
        for f in os.listdir(Config.HEALTH_CHECKS_SCRIPTS_DIR):
            if f in hc_exclude:
                continue
            fpath = path.join(Config.HEALTH_CHECKS_SCRIPTS_DIR, f)
            if path.isfile(fpath) and os.access(fpath, os.X_OK):
                ret = execute(fpath, checkType)
                if len(ret) == 0:
                    continue
                if "success" in ret and not ret["success"]:
                    failingChecks.append(f)
                monitResult[f] = ret

    '''
    Step4: Write results to the json file for admins/management server to read
    '''

    endTime = int(time.time())
    monitResult["lastRun"] = {
        "start": str(datetime.fromtimestamp(startTime)),
        "end": str(datetime.fromtimestamp(endTime)),
        "duration": str(endTime - startTime)
    }

    with open(checkType + "_" + Config.MONITOR_RESULT_FILE_SUFFIX, 'w') as f:
        json.dump(monitResult, f, ensure_ascii=False)

    failChecksFile = Config.FAILING_CHECKS_FILE
    if len(failingChecks) > 0:
        fcs = ""
        for fc in failingChecks:
            fcs = fcs + fc + ","
        fcs = fcs[0, -1]
        with open(failChecksFile, 'w') as f:
            f.write(fcs)
    elif path.isfile(failChecksFile):
        os.remove(failChecksFile)

if __name__ == "__main__":
    checkType = "basic"
    if len(sys.argv) == 2:
        if sys.argv[1] == "advance":
            main("advance")
        elif sys.argv[1] == "basic":
            main("basic")
        else:
            printd("Error: Unknown type of test: " + sys.argv)
    else:
        main("basic")
        main("advance")
