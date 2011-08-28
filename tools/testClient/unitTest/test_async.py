from cloudstackAPI import *
import cloudstackException
import cloudstackTestClient
import sys
import uuid
    
class jobs():
    def __init__(self, zoneId):
        self.zoneId = zoneId
        
    def run(self):
        try:
            cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
            cmd.id = 4
            self.apiClient.destroyVirtualMachine(cmd)
        except cloudstackException.cloudstackAPIException, e:
            print str(e)
        except :
            print sys.exc_info()

if __name__ == "__main__":
    ''' to logging the testclient
    logger = logging.getLogger("test_async")
    fh = logging.FileHandler("test.log")
    logger.addHandler(fh)
    logger.setLevel(logging.DEBUG)
    testclient = cloudstackTestClient.cloudstackTestClient(mgtSvr="localhost", logging=logger)
    '''
    testclient = cloudstackTestClient.cloudstackTestClient(mgtSvr="localhost", port=8080, apiKey="rUJI62HcbyhAXpRgqERZHXlrJz9GiC55fmAm7j4WobLUTFkJyupBm87sbMki1-aRFox7TDs436xYvNW9fTHcew", securityKey="2_SIz9HULx5guCLypSoRoePCBFnTZGIrA3gQ0qhy__oca6dDacJwibMSQh-kVeJivJHeA55AwJZPJAu4U3V5KQ")
    testclient.dbConfigure()
    api = testclient.getApiClient()
    '''
    testclient.submitJob(jobs(1), 10, 10, 1)
    
    js = []
    for i in range(10):
        js.append(jobs(1))
        
    testclient.submitJobs(js, 10, 1)
    '''
    cmds = []
    for i in range(20):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.zoneid =1
        cmd.templateid = 10
        cmd.serviceofferingid = 16
        cmd.displayname = str(uuid.uuid4())
        cmds.append(cmd)
    
    asyncJobResult = testclient.submitCmdsAndWait(cmds, 6)
    
    for jobStatus in asyncJobResult:
        if jobStatus.status:
            print jobStatus.result[0].id, jobStatus.result[0].templatename, jobStatus.startTime, jobStatus.endTime
        else:
            print jobStatus.result, jobStatus.startTime, jobStatus.endTime
            
        print jobStatus.duration
