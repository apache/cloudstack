try:
    import unittest2 as unittest
except ImportError:
    import unittest

from functools import partial
import os
import sys
import logging

module_logger = "testclient.testcase"

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)

class TestCaseExecuteEngine(object):
    def __init__(self, testclient, testCaseFolder, testcaseLogFile=None, testResultLogFile=None):
        self.testclient = testclient
        self.testCaseFolder = testCaseFolder
        self.logger = None
        if testcaseLogFile is not None:
            logger = logging.getLogger("testclient.testcase.TestCaseExecuteEngine")
            fh = logging.FileHandler(testcaseLogFile)
            fh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
            logger.addHandler(fh)
            logger.setLevel(logging.DEBUG)
            self.logger = logger
        if testResultLogFile is not None:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
            self.logger.addHandler(ch)
            fp = open(testResultLogFile, "w")
            self.testResultLogFile = fp
        else:
            self.testResultLogFile = sys.stdout
    
    def injectTestCase(self, testSuites):
        for test in testSuites:
            if isinstance(test, unittest.BaseTestSuite):
                self.injectTestCase(test)
            else:
                setattr(test, "testClient", self.testclient)
                setattr(test, "debug", partial(testCaseLogger, logger=self.logger)) 
    def run(self):
        loader = unittest.loader.TestLoader()
        suite = loader.discover(self.testCaseFolder)
        self.injectTestCase(suite)
        
        unittest.TextTestRunner(stream=self.testResultLogFile, verbosity=2).run(suite)
