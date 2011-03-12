from apisession import ApiSession
from vm import VMCreator

#http://localhost:8080/client/api?_=1303171711292&command=deployVirtualMachine&zoneId=1&hypervisor=KVM&templateId=4&serviceOfferingId=7&response=json&sessionkey=%2Bh3Gh4BffWpQdk4nXmcC88uEk9k%3D

def create_vm():
    vmcreator = VMCreator(api, {'zoneId':1, 'hypervisor':'KVM', 'templateId':4, 
                                'serviceOfferingId':7,
                                'userdata':'dGhpcyBpcyBhIHRlc3QK'})
    vmid = vmcreator.create()
    vmcreator.poll(10, 3)

if __name__ == "__main__":
    api = ApiSession('http://localhost:8080/client/api', 'admin', 'password')
    api.login()

    create_vm()


