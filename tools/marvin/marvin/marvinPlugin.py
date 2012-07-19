from cloudstackTestCase import cloudstackTestCase
from nose.plugins.base import Plugin
from functools import partial
import logging

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)
       
class MarvinPlugin(Plugin):
    """
    Custom test loader for the cloudstackTestCase to be loaded into nose
    """
    
    name = "marvin"
    def configure(self, options, conf):
        self.enabled = 1
        self.enableOpt = "--with-marvin"
        return Plugin.configure(self, options, conf)
    
    def options(self, parser, env):
        Plugin.options(self, parser, env)
 
    def __init__(self):
        Plugin.__init__(self)
        
    def wantClass(self, cls):
        if issubclass(cls, cloudstackTestCase):
            return True
        return None
    
    def loadTestsFromTestCase(self, cls):
        self._injectClients(cls)
        
    def setClient(self, client):
        if client:
            self.testclient = client

    def setClientLog(self, clientlog):
        if clientlog:
            self.log = clientlog

    def _injectClients(self, test):
        testcaselogger = logging.getLogger("testclient.testcase.%s" % test.__class__.__name__)
        fh = logging.FileHandler(self.log) 
        fh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s"))
        testcaselogger.addHandler(fh)
        testcaselogger.setLevel(logging.DEBUG)
        
        setattr(test, "testClient", self.testclient)
        setattr(test, "debug", partial(testCaseLogger, logger=testcaselogger))
        setattr(test, "clstestclient", self.testclient)
        if hasattr(test, "UserName"):
            self.testclient.createNewApiClient(test.UserName, test.DomainName, test.AcctType)
