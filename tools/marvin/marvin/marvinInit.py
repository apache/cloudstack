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
Initializes the marvin and does required prerequisites
for starting it.
   1. Parses the configuration file passed to marvin and creates a
   parsed config.
   2. Initializes the logging required for marvin.All logs are
   now made available under a single timestamped folder.
   3. Deploys the Data Center based upon input.

'''
from marvin.configGenerator import getSetupConfig
from marvin.marvinLog import MarvinLog
from marvin.deployDataCenter import DeployDataCenters
from marvin.cloudstackTestClient import CSTestClient
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.codes import(
    XEN_SERVER,
    SUCCESS,
    FAILED
)
import os


class MarvinInit:

    def __init__(self, config_file,
                 deploy_dc_flag=False,
                 test_mod_name="deploydc",
                 zone=None,
                 hypervisor_type=None,
                 user_logfolder_path=None):
        self.__configFile = config_file
        self.__deployFlag = deploy_dc_flag
        self.__logFolderPath = None
        self.__tcRunLogger = None
        self.__testModName = test_mod_name
        self.__testClient = None
        self.__tcResultFile = None
        self.__testDataFilePath = None
        self.__zoneForTests = zone
        self.__parsedConfig = None
        self.__hypervisorType = hypervisor_type
        self.__userLogFolderPath = user_logfolder_path

    def __parseConfig(self):
        '''
        @Name: __parseConfig
        @Desc : Parses the configuration file passed and assigns
        the parsed configuration
        @Output : SUCCESS or FAILED
        '''
        try:
            if not os.path.isfile(self.__configFile):
                print("\n=== Marvin Parse Config Init Failed ===")
                return FAILED
            self.__parsedConfig = getSetupConfig(self.__configFile)
            print("\n=== Marvin Parse Config Successful ===")
            return SUCCESS
        except Exception as e:
            print("\nException Occurred Under __parseConfig : " \
                  "%s" % GetDetailExceptionInfo(e))
            return FAILED

    def getParsedConfig(self):
        return self.__parsedConfig

    def getLogFolderPath(self):
        return self.__logFolderPath

    def getTestClient(self):
        return self.__testClient

    def getLogger(self):
        return self.__tcRunLogger

    def getResultFile(self):
        '''
        @Name : getDebugFile
        @Desc : Creates the result file at a given path.
        @Output : Returns the Result file to be used for writing
                test outputs
        '''
        if self.__logFolderPath is not None:
            self.__tcResultFile = open(self.__logFolderPath +
                                       "/results.txt", "w")
            return self.__tcResultFile

    def __setHypervisorAndZoneInfo(self):
        '''
        @Name : __setHypervisorAndZoneInfo
        @Desc:  Set the HyperVisor and Zone details;
                default to XenServer
        '''
        try:
            if not self.__hypervisorType:
                if self.__parsedConfig and self.__parsedConfig.zones is not None:
                    for zone in self.__parsedConfig.zones:
                        for pod in zone.pods:
                            if pod is not None:
                                for cluster in pod.clusters:
                                    if cluster is not None and cluster.hypervisor is not None:
                                        self.__hypervisorType = cluster.hypervisor
                                        break
            if not self.__zoneForTests:
                if self.__parsedConfig and self.__parsedConfig.zones is not None:
                    for zone in self.__parsedConfig.zones:
                        self.__zoneForTests = zone.name
                        break
            if not self.__hypervisorType:
                self.__hypervisorType = XEN_SERVER
            return SUCCESS
        except Exception as e:
            print("\n Exception Occurred Under init " \
                  "%s" % GetDetailExceptionInfo(e))
            return FAILED

    def init(self):
        '''
        @Name : init
        @Desc :Initializes the marvin by
               1. Parsing the configuration and creating a parsed config
                  structure
               2. Creates a timestamped log folder and provides all logs
                  to be dumped there
               3. Creates the DataCenter based upon configuration provided
        @Output : SUCCESS or FAILED
        '''
        try:
            print("\n==== Marvin Init Started ====")
            if ((self.__parseConfig() != FAILED) and
                    (self.__setHypervisorAndZoneInfo())and
                    (self.__setTestDataPath() != FAILED) and
                    (self.__initLogging() != FAILED) and
                    (self.__createTestClient() != FAILED) and
                    (self.__deployDC() != FAILED)):
                print("\n==== Marvin Init Successful ====")
                return SUCCESS
            print("\n==== Marvin Init Failed ====")
            return FAILED
        except Exception as e:
            print("\n Exception Occurred Under init " \
                  "%s" % GetDetailExceptionInfo(e))
            return FAILED

    def __initLogging(self):
        '''
        @Name : __initLogging
        @Desc : 1. Initializes the logging for marvin and so provides
                    various log features for automation run.
                    2. Initializes all logs to be available under
                    given Folder Path,where all test run logs
                    are available for a given run.
                    3. All logging like exception log,results, run info etc
                     for a given test run are available under a given
                     timestamped folder
        @Output : SUCCESS or FAILED
        '''
        try:
            log_obj = MarvinLog("CSLog")
            if log_obj:
                ret = log_obj.\
                    createLogs(self.__testModName,
                               self.__parsedConfig.logger,
                               self.__userLogFolderPath)
                if ret != FAILED:
                    self.__logFolderPath = log_obj.getLogFolderPath()
                    self.__tcRunLogger = log_obj.getLogger()
                    print("\n=== Marvin Init Logging Successful===")
                    return SUCCESS
            return FAILED
        except Exception as e:
            print("\n Exception Occurred Under __initLogging " \
                  ":%s" % GetDetailExceptionInfo(e))
            return FAILED

    def __createTestClient(self):
        '''
        @Name : __createTestClient
        @Desc : Creates the TestClient during init
                based upon the parameters provided
        @Output: Returns SUCCESS or FAILED
        '''
        try:
            mgt_details = self.__parsedConfig.mgtSvr[0]
            dbsvr_details = self.__parsedConfig.dbSvr
            self.__testClient = CSTestClient(
                mgt_details,
                dbsvr_details,
                logger=self.__tcRunLogger,
                test_data_filepath=self.__testDataFilePath,
                zone=self.__zoneForTests,
                hypervisor_type=self.__hypervisorType)
            if self.__testClient:
                return self.__testClient.createTestClient()
            return FAILED
        except Exception as e:
            print("\n Exception Occurred Under __createTestClient : %s" % \
                  GetDetailExceptionInfo(e))
            return FAILED

    def __setTestDataPath(self):
        '''
        @Name : __setTestDataPath
        @Desc : Sets the TestData Path for tests to run
        @Output:Returns SUCCESS or FAILED
        '''
        try:
            if ((self.__parsedConfig.TestData is not None) and
                    (self.__parsedConfig.TestData.Path is not None)):
                self.__testDataFilePath = self.__parsedConfig.TestData.Path
            print("\n=== Marvin Setting TestData Successful===")
            return SUCCESS
        except Exception as e:
            print("\nException Occurred Under __setTestDataPath : %s" % \
                  GetDetailExceptionInfo(e))
            return FAILED

    def __deployDC(self):
        '''
        @Name : __deployDC
        @Desc : Deploy the DataCenter and returns accordingly.
        @Output: SUCCESS or FAILED
        '''
        try:
            ret = SUCCESS
            if self.__deployFlag:
                deploy_obj = DeployDataCenters(self.__testClient,
                                               self.__parsedConfig,
                                               self.__tcRunLogger)
                ret = deploy_obj.deploy()
                if ret != SUCCESS:
                    print("==== Deploy DC Failed ====")
            return ret
        except Exception as e:
            print("\n Exception Occurred Under __deployDC : %s" % \
                  GetDetailExceptionInfo(e))
            return FAILED
