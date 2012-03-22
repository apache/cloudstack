try:
    import unittest2 as unittest
except ImportError:
    import unittest

from functools import partial
import os
import sys
import logging

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)

class TestCaseExecuteEngine(object):
    def __init__(self, testclient, testCaseFolder, testcaseLogFile=None, testResultLogFile=None):
        self.testclient = testclient
        self.testCaseFolder = testCaseFolder
        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")

        if testcaseLogFile is not None:
            self.logfile = testcaseLogFile
            self.logger = logging.getLogger("TestCaseExecuteEngine")
            fh = logging.FileHandler(self.logfile) 
            fh.setFormatter(self.logformat)
            self.logger.addHandler(fh)
            self.logger.setLevel(logging.DEBUG)
        if testResultLogFile is not None:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(self.logformat)
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
                #logger bears the name of the test class
                testcaselogger = logging.getLogger("testclient.testcase.%s"%test.__class__.__name__)
                fh = logging.FileHandler(self.logfile) 
                fh.setFormatter(self.logformat)
                testcaselogger.addHandler(fh)
                testcaselogger.setLevel(logging.DEBUG)
                
                #inject testclient and logger into each unittest 
                setattr(test, "testClient", self.testclient)
                setattr(test, "debug", partial(testCaseLogger, logger=testcaselogger))
                setattr(test.__class__, "clstestclient", self.testclient)
                
    def run(self):
        loader = unittest.loader.TestLoader()
        suite = loader.discover(self.testCaseFolder)
        self.injectTestCase(suite)
        
        unittest.TextTestRunner(stream=self.testResultLogFile, verbosity=2).run(suite)
