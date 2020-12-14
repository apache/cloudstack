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
@Desc: Module for providing logging facilities to marvin
'''
import logging
import sys
import time
import os
from marvin.codes import (SUCCESS,
                          FAILED
                          )
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.lib.utils import random_gen


class MarvinLog:

    '''
    @Name  : MarvinLog
    @Desc  : provides interface for logging to marvin
    @Input : logger_name : name for logger
    '''
    logFormat = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
    _instance = None

    def __new__(cls, logger_name):
        if not cls._instance:
            cls._instance = super(MarvinLog, cls).__new__(cls, logger_name)
            return cls._instance

    def __init__(self, logger_name):
        '''
        @Name: __init__
        @Input: logger_name for logger
        '''
        self.__loggerName = logger_name
        '''
        Logger for Logging Info
        '''
        self.__logger = None
        '''
        Log Folder Directory
        '''
        self.__logFolderDir = None
        self.__setLogger()

    def __setLogger(self):
        '''
        @Name : __setLogger
        @Desc : Sets the Logger and Level
        '''
        self.__logger = logging.getLogger(self.__loggerName)
        self.__logger.setLevel(logging.DEBUG)

    def __setLogHandler(self, log_file_path,
                        log_format=None,
                        log_level=logging.DEBUG):
        '''
        @Name : __setLogHandler
        @Desc: Adds the given Log handler to the current logger
        @Input: log_file_path: Log File Path as where to store the logs
               log_format : Format of log messages to be dumped
               log_level : Determines the level of logging for this logger
        @Output: SUCCESS if no issues else FAILED
        '''
        try:
            if log_file_path is not None:
                stream = logging.FileHandler(log_file_path)
            else:
                stream = logging.StreamHandler(stream=sys.stdout)

            if log_format is not None:
                stream.setFormatter(log_format)
            else:
                stream.setFormatter(self.__class__.logFormat)
            stream.setLevel(log_level)
            self.__logger.addHandler(stream)
            return SUCCESS
        except Exception as e:
            print("\nException Occurred Under " \
                  "__setLogHandler %s" % GetDetailExceptionInfo(e))
            return FAILED

    def __cleanPreviousLogs(self, logfolder_to_remove):
        '''
        @Name : __cleanPreviousLogs
        @Desc : Removes the Previous Logs
        @Return: N\A
        @Input: logfolder_to_remove: Path of Log to remove
        '''
        try:
            if os.path.isdir(logfolder_to_remove):
                os.rmdir(logfolder_to_remove)
        except Exception as e:
            print("\n Exception Occurred Under __cleanPreviousLogs :%s" % \
                  GetDetailExceptionInfo(e))
            return FAILED

    def getLogger(self):
        '''
        @Name:getLogger
        @Desc : Returns the Logger
        '''
        return self.__logger

    def getLogFolderPath(self):
        '''
        @Name : getLogFolderPath
        @Desc : Returns the final log directory path for marvin run
        '''
        return self.__logFolderDir

    def createLogs(self,
                   test_module_name=None,
                   log_cfg=None,
                   user_provided_logpath=None, use_temp_path=True):
        '''
        @Name : createLogs
        @Desc : Gets the Logger with file paths initialized and created
        @Inputs :test_module_name: Test Module Name to use for logs while
                 creating log folder path
                 log_cfg: Log Configuration provided inside of
                 Configuration
                 user_provided_logpath:LogPath provided by user
                                       If user provided log path
                                       is available, then one in cfg
                                       will  not be picked up.
                 use_temp_path: Boolean value which specifies either logs will
                                       be prepended by random path or not.
        @Output : SUCCESS\FAILED
        '''
        try:
            temp_ts = time.strftime("%b_%d_%Y_%H_%M_%S", time.localtime())

            if test_module_name is None:
                temp_path = temp_ts + "_" + random_gen()
            else:
                temp_path = str(test_module_name) + "__" + str(temp_ts) + "_" + random_gen()

            if user_provided_logpath:
                temp_dir = os.path.join(user_provided_logpath, "MarvinLogs")
            elif ((log_cfg is not None) and
                    ('LogFolderPath' in list(log_cfg.__dict__.keys())) and
                    (log_cfg.__dict__.get('LogFolderPath') is not None)):
                temp_dir = os.path.join(log_cfg.__dict__.get('LogFolderPath'), "MarvinLogs")

            if use_temp_path == True:
                self.__logFolderDir = os.path.join(temp_dir, temp_path)
            else:
                if test_module_name == None:
                    self.__logFolderDir = temp_dir
                else:
                    self.__logFolderDir = os.path.join(temp_dir, str(test_module_name))

            print("\n==== Log Folder Path: %s. All logs will be available here ====" % str(self.__logFolderDir))
            os.makedirs(self.__logFolderDir)

            '''
            Log File Paths
            1. FailedExceptionLog
            2. RunLog contains the complete Run Information for Test Run
            3. ResultFile contains the TC result information for Test Run
            '''

            tc_failed_exception_log = os.path.join(self.__logFolderDir, "failed_plus_exceptions.txt")
            tc_run_log = os.path.join(self.__logFolderDir, "runinfo.txt")

            if self.__setLogHandler(tc_run_log,
                                    log_level=logging.DEBUG) != FAILED:
                self.__setLogHandler(tc_failed_exception_log,
                                     log_level=logging.FATAL)
                return SUCCESS
            return FAILED
        except Exception as e:
            print("\n Exception Occurred Under createLogs :%s" % \
                  GetDetailExceptionInfo(e))
            return FAILED
