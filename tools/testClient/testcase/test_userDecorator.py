from cloudstackTestCase import *

#@UserName('user', 'LEAF', 0)
#@UserName('user0', 'LEAF', 0)
#@UserName('user', 'LEAF0', 0)
#@UserName('admin', 'ROOT', 1)
@UserName('domadmin', 'LEAF0', 2)
class TestUserDecorator(cloudstackTestCase):

    def test_listUserTemplates(self):
        apiClient = self.testClient.getApiClient()
        listtmcmd = listTemplates.listTemplatesCmd()
        listtmcmd.zoneid = 1
        listtmcmd.templatefilter = "self"
        listtmresponse = apiClient.listTemplates(listtmcmd)
        if listtmresponse is not None:
            self.assertEqual(len(listtmresponse), 0, "there should be no user created templates")
            self.debug("%d user templates found"%len(listtmresponse))
            
    def test_listFeaturedTemplates(self):
        apiClient = self.testClient.getApiClient()
        listtmcmd = listTemplates.listTemplatesCmd()
        listtmcmd.zoneid = 1
        listtmcmd.templatefilter = "featured"
        listtmresponse = apiClient.listTemplates(listtmcmd)
        if listtmresponse is not None:
            self.assertNotEqual(len(listtmresponse), 0, "there should default featured templates")
            self.debug("%d featured templates found"%len(listtmresponse))
            return listtmresponse[0].id
            
    def test_listVirtualMachines(self):
        apiClient = self.testClient.getApiClient()
        listvmcmd = listVirtualMachines.listVirtualMachinesCmd()
        listvmcmd.zoneid = 1
        listvmcmd.state = "Running"
        listvmresponse = apiClient.listVirtualMachines(listvmcmd)
        if listvmresponse is not None and len(listvmresponse) > 0:
            self.debug("%d running VMs found"%len(listvmresponse))
        else:
            self.debug("No Vms Found")
            
    def test_deployVM(self):
        apiClient = self.testClient.getApiClient()
        listzones = listZones.listZonesCmd()
        listzoneres = apiClient.listZones(listzones)
        zoneid = listzoneres[0].id
        
        listservice = listServiceOfferings.listServiceOfferingsCmd()
        listserviceres = apiClient.listServiceOfferings(listservice)
        serviceofferingid = listserviceres[0].id
        
        deploycmd = deployVirtualMachine.deployVirtualMachineCmd()
        deploycmd.displayname = "testVm"
        deploycmd.templateid = self.test_listFeaturedTemplates()
        deploycmd.zoneid = zoneid
        deploycmd.serviceofferingid = serviceofferingid
        apiClient.deployVirtualMachine(deploycmd)
