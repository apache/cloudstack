from cloudstackTestCase import *
class TestCase2(cloudstackTestCase):

    def test_cloudstackapi1(self):
        apiClient = self.testClient.getApiClient()
        listtmcmd = listTemplates.listTemplatesCmd()
        listtmcmd.id = 10
        listtmcmd.zoneid = 1
        listtmcmd.templatefilter = "featured"
        #listtmresponse = apiClient.listTemplates(listtmcmd)
        #self.debug(listtmresponse[0].isready)

        listhostcmd=listHosts.listHostsCmd()
        listhostcmd.zoneid=1
        listhostcmd.type="Routing"
        
        asyncJobResult=self.testClient.submitCmdsAndWait([listhostcmd],1)
        listVMresponse = asyncJobResult[0].result
        self.debug("Total Number of Hosts: " + str(len(listVMresponse)))
    
        for i in listVMresponse:
            self.debug("id: " + str(i.id) +" pod id: " + str(i.podid) +" host tag: " + str(i.hosttags))
        