from cloudstackTestCase import *
import uuid
class TestCase1(cloudstackTestCase):
    def setUp(self):
        '''get a small service offering id'''
        listsocmd = listServiceOfferings.listServiceOfferingsCmd()
        listsocmd.name = "Small Instance"
        listsocmd.issystem = "false"
        sos = self.testClient.getApiClient().listServiceOfferings(listsocmd)
        if sos is not None and len(sos) > 0:
            self.svid = sos[0].id
        listdiskovcmd = listDiskOfferings.listDiskOfferingsCmd()
        listdiskovcmd.name = "Small"
        disoov = self.testClient.getApiClient().listDiskOfferings(listdiskovcmd)
        if disoov is not None and len(disoov) > 0:
            self.diskov = disoov[0].id
            
        '''get user vm template id'''
        listtmplcmd = listTemplates.listTemplatesCmd()
        listtmplcmd.templatefilter = "featured"
        tmpls = self.testClient.getApiClient().listTemplates(listtmplcmd)
        if tmpls is not None:
            for tmpl in tmpls:
                if tmpl.isready == "true":
                    self.templateId = tmpl.id
                    self.zoneId = tmpl.zoneid
                    break
        
    def test_cloudstackapi(self):
        apiClient = self.testClient.getApiClient()
       
        createvm = deployVirtualMachine.deployVirtualMachineCmd()
        createvm.serviceofferingid = self.svid
        createvm.templateid = self.templateId
        createvm.zoneid = self.zoneId
        vm = apiClient.deployVirtualMachine(createvm)
        vmId = vm.id
        
        creatvolume = createVolume.createVolumeCmd()
        creatvolume.name = "tetst" + str(uuid.uuid4())
        creatvolume.diskofferingid = self.diskov
        creatvolume.zoneid = self.zoneId
        createvolumeresponse = apiClient.createVolume(creatvolume)
        volumeId = createvolumeresponse.id
        attach = attachVolume.attachVolumeCmd()
 
        attach.id = volumeId
        attach.virtualmachineid = vmId
        apiClient.attachVolume(attach)