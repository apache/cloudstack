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
'''
@Desc: Initializes the marvin and does required prerequisites
for starting it.
1. Parses the configuration file passed to marvin and creates a
   parsed config
   2. Initializes the logging required for marvin.All logs are
   now made available under a single timestamped folder.
   3. Deploys the Data Center based upon input

'''

from marvin import configGenerator
from marvin import cloudstackException
from marvin.marvinLog import MarvinLog
from marvin.deployDataCenter import deployDataCenters
from marvin.codes import(
    YES,
    NO,
    SUCCESS,
    FAILED
    )
import sys
import time
import os
import logging


class MarvinInit:
    def __init__(self, config_file, load_flag):
        self.__configFile = config_file
        self.__loadFlag = load_flag
        self.__parsedConfig = None
        self.__logFolderPath = None
        self.__tcRunLogger = None
        self.__testClient = None
        self.__tcRunDebugFile = None

    def __parseConfig(self):
        '''
        @Desc : Parses the configuration file passed and assigns
        the parsed configuration
        '''
        try:
            self.__parsedConfig = configGenerator.\
                getSetupConfig(self.__configFile)
            return SUCCESS
        except Exception, e:
            print "\n Exception Occurred Under __parseConfig : %s" % str(e)
            return None

    def getParsedConfig(self):
        return self.__parsedConfig

    def getLogFolderPath(self):
        return self.__logFolderPath

    def getTestClient(self):
        return self.__testClient

    def getLogger(self):
        return self.__tcRunLogger

    def getDebugFile(self):
        return self.__tcRunDebugFile

    def init(self):
        '''
        @Desc :Initializes the marvin by
               1. Parsing the configuration and creating a parsed config
                  structure
               2. Creates a timestamped log folder and provides all logs
                  to be dumped there
               3. Creates the DataCenter based upon configuration provided
        '''
        try:
            if ((self.__parseConfig() is not None) and
               (self.__initLogging() is not None) and
               (self.__deployDC() is not None)):
                return SUCCESS
            else:
                return FAILED
        except Exception, e:
            print "\n Exception Occurred Under init %s" % str(e)
            return FAILED

    def __initLogging(self):
        try:
            '''
            @Desc : 1. Initializes the logging for marvin and so provides
                    various log features for automation run.
                    2. Initializes all logs to be available under
                    given Folder Path,where all test run logs
                    are available for a given run.
                    3. All logging like exception log,results, run info etc
                     for a given test run are available under a given
                     timestamped folder
            '''
            log_config = self.__parsedConfig.logger
            temp_path = "".join(str(time.time()).split("."))
            if log_config is not None:
                if log_config.LogFolderPath is not None:
                    self.logFolderPath = log_config.LogFolderPath + temp_path
                else:
                    self.logFolderPath = temp_path
            else:
                self.logFolderPath = temp_path
            os.makedirs(self.logFolderPath)
            '''
            Log File Paths
            '''
            tc_failed_exceptionlog = self.logFolderPath + "/failed_" \
                                                          "plus_" \
                                                          "exceptions.txt"
            tc_run_log = self.logFolderPath + "/runinfo.txt"
            self.__tcRunDebugFile = open(self.logFolderPath +
                                         "/results.txt", "w")

            log_obj = MarvinLog("CSLog")
            self.__tcRunLogger = log_obj.setLogHandler(tc_run_log)
            log_obj.setLogHandler(tc_failed_exceptionlog,
                                  log_level=logging.FATAL)
            return SUCCESS
        except Exception, e:
            print "\n Exception Occurred Under __initLogging :%s" % str(e)
            return None

    def __deployDC(self):
        try:
            '''
            Deploy the DataCenter and retrieves test client.
            '''
            deploy_obj = deployDataCenters(self.__parsedConfig,
                                           self.__tcRunLogger)
            if self.__loadFlag:
                deploy_obj.loadCfg()
            else:
                deploy_obj.deploy()

            self.__testClient = deploy_obj.testClient
            return SUCCESS
        except Exception, e:
            print "\n Exception Occurred Under __deployDC : %s" % str(e)
            return None
