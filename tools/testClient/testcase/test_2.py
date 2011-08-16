import unittest
import random
from cloudstackAPI import *
class TestCase2(unittest.case.TestCase):

    def test_cloudstackapi1(self):
        apiClient = self.testClient.getApiClient()
        listtmcmd = listTemplates.listTemplatesCmd()
        listtmcmd.id = 10
        listtmcmd.zoneid = 1
        listtmcmd.templatefilter = "featured"
        listtmresponse = apiClient.listTemplates(listtmcmd)
        self.debug(listtmresponse[0].isready)
