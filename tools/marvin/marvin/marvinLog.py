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
from marvin.codes import (NO,
                          YES
                          )


class MarvinLog:
    '''
    @Desc  : provides interface for logging to marvin
    @Input : logger_name : name for logger
    '''
    logFormat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")
    _instance = None

    def __new__(cls, logger_name):
        if not cls._instance:
            cls._instance = super(MarvinLog, cls).__new__(cls, logger_name)
            return cls._instance

    def __init__(self, logger_name):
        self.loggerName = logger_name
        self.logger = None
        self.__setLogger()

    def __setLogger(self):
        self.logger = logging.getLogger(self.loggerName)
        self.logger.setLevel(logging.DEBUG)

    def setLogHandler(self, log_file_path, log_format=None,
                      log_level=logging.DEBUG):
        '''
        @Desc: Adds the given Log handler to the current logger
        @Input: log_file_path: Log File Path as where to store the logs
               log_format : Format of log messages to be dumped
               log_level : Determines the level of logging for this logger
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
            self.logger.addHandler(stream)
        except Exception, e:
            print "\n Exception Occurred Under setLogHandler %s" % str(e)
        finally:
            return self.logger
