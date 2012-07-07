from functools import partial
import unittest
import nose
import nose.core
import os
import sys
import logging
import xmlrunner
from cloudstackTestCase import cloudstackTestCase

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)
        
class NoseCloudStackTestLoader(nose.loader.TestLoader):
    """
    Custom test loader for the cloudstackTestCase to be loaded into nose
    """
    
    def loadTestsFromTestCase(self, testCaseClass):
        if issubclass(testCaseClass, cloudstackTestCase):
            testCaseNames = self.getTestCaseNames(testCaseClass)
            tests = []
            for testCaseName in testCaseNames:
                testCase = testCaseClass(testCaseName)
                tests.append(testCase)
            return self.suiteClass(tests)
        else:
            return super(NoseCloudStackTestLoader, self).loadTestsFromTestCase(testCaseClass)
    
    def loadTestsFromName(self, name, module=None, discovered=False):
        return nose.loader.TestLoader.loadTestsFromName(self, name, module=module, discovered=discovered)
    
    def loadTestsFromNames(self, names, module=None):
        return nose.loader.TestLoader.loadTestsFromNames(self, names, module=module)
            

class NoseTestExecuteEngine(object):
    """
    Runs the CloudStack tests using nose as the execution engine
    """
    
    def __init__(self, testclient=None, workingdir=None, filename=None, clientLog=None, resultLog=None, format="text"):
        self.testclient = testclient
        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")
        self.suite = []

        if clientLog is not None:
            self.logfile = clientLog
            self.logger = logging.getLogger("NoseTestExecuteEngine")
            fh = logging.FileHandler(self.logfile) 
            fh.setFormatter(self.logformat)
            self.logger.addHandler(fh)
            self.logger.setLevel(logging.DEBUG)
        if resultLog is not None:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(self.logformat)
            self.logger.addHandler(ch)
            fp = open(resultLog, "w")
            self.testResultLogFile = fp
        else:
            self.testResultLogFile = sys.stderr
 
        if workingdir is not None:
            self.loader = NoseCloudStackTestLoader()
            self.suite = self.loader.loadTestsFromName(workingdir)
            for test in self.suite:
                self.injectClients(test)
        elif filename is not None:
            self.loader = NoseCloudStackTestLoader()
            self.suite = self.loader.loadTestsFromFile(filename)
            for test in self.suite:
                self.injectClients(test)
        else:
            raise EnvironmentError("Need to give either a test directory or a test file")
        
        if format == "text":
            self.runner = nose.core.TextTestRunner(stream=self.testResultLogFile, descriptions=1, verbosity=2, config=None)
        else:
            self.runner = xmlrunner.XMLTestRunner(output='xml-reports', verbose=True)
            
    def runTests(self):
         nose.core.TestProgram(argv=["--process-timeout=3600"], testRunner=self.runner, testLoader=self.loader)
        
    def injectClients(self, test):
        testcaselogger = logging.getLogger("testclient.testcase.%s"%test.__class__.__name__)
        fh = logging.FileHandler(self.logfile) 
        fh.setFormatter(self.logformat)
        testcaselogger.addHandler(fh)
        testcaselogger.setLevel(logging.DEBUG)
        
        setattr(test, "testClient", self.testclient)
        setattr(test, "debug", partial(testCaseLogger, logger=testcaselogger))
        setattr(test.__class__, "clstestclient", self.testclient)
        if hasattr(test, "UserName"):
            self.testclient.createNewApiClient(test.UserName, test.DomainName, test.AcctType)
