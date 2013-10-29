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

import marvin
import sys
import logging
import nose.core
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin import deployDataCenter
from nose.plugins.base import Plugin
import time


class MarvinPlugin(Plugin):
    """
    Custom plugin for the cloudstackTestCases to be run using nose
    """

    name = "marvin"

    def configure(self, options, config):
        """enable the marvin plugin when the --with-marvin directive is given
        to nose. The enableOpt value is set from the command line directive and
        self.enabled (True|False) determines whether marvin's tests will run.
        By default non-default plugins like marvin will be disabled
        """
        if hasattr(options, self.enableOpt):
            if not getattr(options, self.enableOpt):
                self.enabled = False
                return
            else:
                self.enabled = True

        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - " +
                                           "%(name)s - %(message)s")

        if options.debug_log:
            self.logger = logging.getLogger("NoseTestExecuteEngine")
            self.debug_stream = logging.FileHandler(options.debug_log)
            self.debug_stream.setFormatter(self.logformat)
            self.logger.addHandler(self.debug_stream)
            self.logger.setLevel(logging.DEBUG)

        if options.result_log:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(self.logformat)
            self.logger.addHandler(ch)
            self.result_stream = open(options.result_log, "w")
        else:
            self.result_stream = sys.stdout

        deploy = deployDataCenter.deployDataCenters(options.config_file)
        deploy.loadCfg() if options.load else deploy.deploy()
        self.setClient(deploy.testClient)
        self.setConfig(deploy.getCfg())

        self.testrunner = nose.core.TextTestRunner(stream=self.result_stream,
                                                   descriptions=True,
                                                   verbosity=2, config=config)

    def options(self, parser, env):
        """
        Register command line options
        """
        parser.add_option("--marvin-config", action="store",
                          default=env.get('MARVIN_CONFIG', './datacenter.cfg'),
                          dest="config_file",
                          help="Marvin's configuration file where the " +
                               "datacenter information is specified " +
                               "[MARVIN_CONFIG]")
        parser.add_option("--result-log", action="store",
                          default=env.get('RESULT_LOG', None),
                          dest="result_log",
                          help="The path to the results file where test " +
                               "summary will be written to [RESULT_LOG]")
        parser.add_option("--client-log", action="store",
                          default=env.get('DEBUG_LOG', 'debug.log'),
                          dest="debug_log",
                          help="The path to the testcase debug logs " +
                          "[DEBUG_LOG]")
        parser.add_option("--load", action="store_true", default=False,
                          dest="load",
                          help="Only load the deployment configuration given")

        Plugin.options(self, parser, env)

    def __init__(self):
        self.identifier = None
        Plugin.__init__(self)

    def prepareTestRunner(self, runner):
        return self.testrunner

    def wantClass(self, cls):
        if cls.__name__ == 'cloudstackTestCase':
            return False
        if issubclass(cls, cloudstackTestCase):
            return True
        return None

    def loadTestsFromTestCase(self, cls):
        if cls.__name__ != 'cloudstackTestCase':
            self.identifier = cls.__name__
            self._injectClients(cls)

    def setClient(self, client):
        if client is not None:
            self.testclient = client

    def setConfig(self, config):
        if config is not None:
            self.config = config

    def beforeTest(self, test):
        self.testName = test.__str__().split()[0]
        self.testclient.identifier = '-'.join([self.identifier, self.testName])
        self.logger.name = test.__str__()

    def startTest(self, test):
        """
        Currently used to record start time for tests
        """
        self.startTime = time.time()

    def stopTest(self, test):
        """
        Currently used to record end time for tests
        """
        endTime = time.time()
        if self.startTime is not None:
            totTime = int(endTime - self.startTime)
            self.logger.debug(
                "TestCaseName: %s; Time Taken: %s Seconds; \
                StartTime: %s; EndTime: %s"
                % (self.testName, str(totTime),
                   str(time.ctime(self.startTime)), str(time.ctime(endTime))))

    def _injectClients(self, test):
        setattr(test, "debug", self.logger.debug)
        setattr(test, "info", self.logger.info)
        setattr(test, "warn", self.logger.warning)
        setattr(test, "testClient", self.testclient)
        setattr(test, "config", self.config)
        if self.testclient.identifier is None:
            self.testclient.identifier = self.identifier
        setattr(test, "clstestclient", self.testclient)
        if hasattr(test, "user"):
            # when the class-level attr applied. all test runs as 'user'
            self.testclient.createUserApiClient(test.UserName, test.DomainName,
                                                test.AcctType)
