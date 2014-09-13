__author__ = 'edison'
import os
from marvin.marvinPlugin import MarvinInit
from marvin.codes import FAILED

def getMarvin():
    configFile = os.environ.get("MARVIN_CONFIG", os.path.join(os.path.dirname(os.path.realpath(__file__)),"..", "..", "..", "setup", "dev", "advanced.cfg"))
    deployDcb = False
    deployDc = os.environ.get("MARVIN_DEPLOY_DC", "false")
    if deployDc in ["True", "true"]:
        deployDcb = True
    zoneName = os.environ.get("MARVIN_ZONE_NAME", "Sandbox-simulator")
    hypervisor_type = os.environ.get("MARVIN_HYPERVISOR_TYPE", "simulator")
    logFolder = os.environ.get("MARVIN_LOG_FOLDER", os.path.expanduser(os.path.join("~","marvin")))

    marvinObj = MarvinInit(configFile,
               deployDcb,
               None,
               zoneName,
               hypervisor_type,
               logFolder)

    result = marvinObj.init()
    if result == FAILED:
        return None
    else:
        return marvinObj

def initTestClass(cls, idenifier):
    marvinObj = None
    if hasattr(cls, "marvinObj"):
        marvinObj = cls.marvinObj
    else:
        marvinObj = getMarvin()
    setattr(cls, "debug", marvinObj.getLogger().debug)
    setattr(cls, "info", marvinObj.getLogger().info)
    setattr(cls, "warn", marvinObj.getLogger().warning)
    setattr(cls, "error",marvinObj.getLogger().error)
    setattr(cls, "testClient", marvinObj.getTestClient())
    setattr(cls, "config", marvinObj.getParsedConfig())
    if hasattr(cls, "clstestclient") is not True:
        setattr(cls, "clstestclient", marvinObj.getTestClient())
    if cls.clstestclient is None:
        cls.clstestclient = marvinObj.getTestClient()

    marvinObj.getTestClient().identifier = idenifier
    if hasattr(cls, "user"):
        # when the class-level attr applied. all test runs as 'user'
        cls.testClient.getUserApiClient(cls.UserName,
                                           cls.DomainName,
                                           cls.AcctType)