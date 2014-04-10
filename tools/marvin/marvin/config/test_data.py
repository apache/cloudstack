test_data = {
        "region": {
                "regionid": "2",
                "regionname": "Region2",
                "regionendpoint": "http://region2:8080/client"
        },
        "zone": "NA",
        "hypervisor": "XenServer",
        "vdomain": { "name": "domain" },
	    "email" : "test@test.com",
	    "gateway" : "172.1.1.1",
        "netmask" : "255.255.255.0",
        "startip" : "172.1.1.10",
        "endip" : "172.1.1.20",
        "regionid" : "1",
        "vlan" :"10",
        "isportable" : "true",       
 
        "project": {
            "name": "Project",
            "displaytext": "Test project"
        },
        "account": {
            "email": "test-account@test.com",
            "firstname": "test",
            "lastname": "test",
            "username": "test-account",
            "password": "password"
        }, 
        "small": {
            "displayname": "testserver",
            "username": "root", 
            "password": "password",
            "ssh_port": 22,
            "hypervisor": "XenServer",
            "privateport": 22,
            "publicport": 22,
            "protocol": 'TCP',
        },
        "medium": {
            "displayname": "testserver",
            "username": "root",
            "password": "password",
            "ssh_port": 22,
            "hypervisor": 'XenServer',
            "privateport": 22,
            "publicport": 22,
            "protocol": 'TCP',
        },
        "service_offerings": {
            "name": "Tiny Instance",
            "displaytext": "Tiny Instance",
            "cpunumber": 1,
            "cpuspeed": 100,
            "memory": 128,
            
            "tiny": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100, 
                "memory": 128, 
            },   
            "small": {
                "name": "Small Instance",
                "displaytext": "Small Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256 
            },
            "medium": {
                "name": "Medium Instance",
                "displaytext": "Medium Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256,
            },
            "big": {
                "name": "BigInstance",
                "displaytext": "BigInstance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 512,
            }
        },
        "disk_offering": {
            "name": "Disk offering",
            "displaytext": "Disk offering",
            "disksize": 1  
        },
        'resized_disk_offering': {
            "displaytext": "Resized",
            "name": "Resized",
            "disksize": 3
        },
        "network": {
            "name": "Test Network",
            "displaytext": "Test Network",
            "acltype": "Account",
        },
        "network2": {
            "name": "Test Network Shared",
            "displaytext": "Test Network Shared",
            "vlan" :1201,
            "gateway" :"172.16.15.1",
            "netmask" :"255.255.255.0",
            "startip" :"172.16.15.21",
            "endip" :"172.16.15.41",
            "acltype": "Account",
        },
        "network_offering": {
            "name": 'Test Network offering',
            "displaytext": 'Test Network offering',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "serviceProviderList" : {
                "Dhcp": 'VirtualRouter',
                "Dns": 'VirtualRouter',
                "SourceNat": 'VirtualRouter',
                "PortForwarding": 'VirtualRouter',
            },
        },
        "isolated_network_offering": {
            "name": "Network offering-DA services",
            "displaytext": "Network offering-DA services",
            "guestiptype": "Isolated",
            "supportedservices": "Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat",
            "traffictype": "GUEST",
            "availability": "Optional'",
            "serviceProviderList": {
                "Dhcp": "VirtualRouter",
                "Dns": "VirtualRouter",
                "SourceNat": "VirtualRouter",
                "PortForwarding": "VirtualRouter",
                "Vpn": "VirtualRouter",
                "Firewall": "VirtualRouter",
                "Lb": "VirtualRouter",
                "UserData": "VirtualRouter",
                "StaticNat": "VirtualRouter"
            }
        },
        "isolated_network": {
            "name": "Isolated Network",
            "displaytext": "Isolated Network"
        },
        "virtual_machine": {
            "displayname": "Test VM",
            "username": "root",
            "password": "password",
            "ssh_port": 22,
            "privateport": 22,
            "publicport": 22,
            "protocol": "TCP",
            "affinity": {
                "name": "webvms",
                "type": "host anti-affinity",
            },
        },
        "virtual_machine2" : {
             "name" : "testvm2",
             "displayname" : "Test VM2",
        },
        "server_without_disk": {
            "displayname": "Test VM-No Disk",
            "username": "root",
            "password": "password",
            "ssh_port": 22,
            "hypervisor": 'XenServer',
            "privateport": 22,
            "publicport": 22,
            "protocol": 'TCP',
        },
        "shared_network": {
            "name": "MySharedNetwork - Test",
            "displaytext": "MySharedNetwork",
            "vlan" : "",
            "gateway" :"",
            "netmask" :"",
            "startip" :"",
            "endip" :"",
            "acltype" : "Domain",
            "scope":"all"
        },
        "shared_network_offering_sg": {
            "name": "MySharedOffering-sg",
            "displaytext": "MySharedOffering-sg",
            "guestiptype": "Shared",
            "supportedservices": "Dhcp,Dns,UserData,SecurityGroup",
            "specifyVlan" : "False",
            "specifyIpRanges" : "False",
            "traffictype": "GUEST",
            "serviceProviderList" : {
                "Dhcp": "VirtualRouter",
                "Dns": "VirtualRouter",
                "UserData": "VirtualRouter",
                "SecurityGroup": "SecurityGroupProvider"
            }
        },
        "shared_network_sg": {
            "name": "Shared-Network-SG-Test",
            "displaytext": "Shared-Network_SG-Test",
            "networkofferingid":"1",
            "vlan" : "",
            "gateway" :"",
            "netmask" :"255.255.255.0",
            "startip" :"",
            "endip" :"",
            "acltype" : "Domain",
            "scope":"all"
        },
        "vpc_offering": {
            "name": "VPC off",
            "displaytext": "VPC off",
            "supportedservices": "Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,NetworkACL"
        },
        "vpc": {
            "name": "TestVPC",
            "displaytext": "TestVPC",
            "cidr": "10.0.0.1/24"
        },
        "clusters": {
            0: {
                "clustername": "Xen Cluster",
                "clustertype": "CloudManaged",
                "hypervisor": "XenServer",
            },
            1: {
                "clustername": "KVM Cluster",
                "clustertype": "CloudManaged",
                "hypervisor": "KVM",
            },
            2: {
                "hypervisor": 'VMware',
                "clustertype": 'ExternalManaged',
                "username": 'administrator',
                "password": 'fr3sca',
                "url": 'http://192.168.100.17/CloudStack-Clogeny-Pune/Pune-1',
                "clustername": 'VMWare Cluster',
            },
        },
        "hosts": {
            "xenserver": {
                "hypervisor": 'XenServer',
                "clustertype": 'CloudManaged',
                "url": 'http://192.168.100.211',
                "username": "root",
                "password": "fr3sca",
            },
            "kvm": {
                "hypervisor": 'KVM',
                "clustertype": 'CloudManaged',
                "url": 'http://192.168.100.212',
                "username": "root",
                "password": "fr3sca",
            },
            "vmware": {
                "hypervisor": 'VMware',
                "clustertype": 'ExternalManaged',
                "url": 'http://192.168.100.203',
                "username": "administrator",
                "password": "fr3sca",
            },
        },
        "network_offering_shared": {
            "name": 'Test Network offering shared',
            "displaytext": 'Test Network offering Shared',
            "guestiptype": 'Shared',
            "supportedservices": 'Dhcp,Dns,UserData',
            "traffictype": 'GUEST',
            "specifyVlan" : "True",
            "specifyIpRanges" : "True",
            "serviceProviderList" : {
                "Dhcp": 'VirtualRouter',
                "Dns": 'VirtualRouter',
                "UserData": 'VirtualRouter',
            },
        },        
        "network_offering_internal_lb": {
            "name": "Network offering for internal lb service",
            "displaytext": "Network offering for internal lb service",
            "guestiptype": "Isolated",
            "traffictype": "Guest",
            "supportedservices": "Vpn,Dhcp,Dns,Lb,UserData,SourceNat,StaticNat,PortForwarding,NetworkACL",
            "serviceProviderList": {
                "Dhcp": "VpcVirtualRouter",
                "Dns": "VpcVirtualRouter",
                "Vpn": "VpcVirtualRouter",
                "UserData": "VpcVirtualRouter",
                "Lb": "InternalLbVM",
                "SourceNat": "VpcVirtualRouter",
                "StaticNat": "VpcVirtualRouter",
                "PortForwarding": "VpcVirtualRouter",
                "NetworkACL": "VpcVirtualRouter",
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "peraccount"},
                "Lb": {"lbSchemes": "internal", "SupportedLbIsolation": "dedicated"}
            }
        },
        "natrule": {
            "privateport": 22,
            "publicport": 2222,
            "protocol": "TCP"
        },
        "lbrule": {
            "name": "SSH",
            "alg": "roundrobin",
            "privateport": 22,
            "publicport": 2222,
            "protocol": 'TCP'
        },

        "iso1": {
            "displaytext": "Test ISO 1",
            "name": "ISO 1",
            "url": "http://people.apache.org/~tsp/dummy.iso",
            "isextractable": True,
            "isfeatured": True,
            "ispublic": True,
            "ostype": "CentOS 5.6 (64-bit)",
        },
        "iso2": {
            "displaytext": "Test ISO 2",
            "name": "ISO 2",
            "url": "http://people.apache.org/~tsp/dummy.iso",
            "isextractable": True,
            "isfeatured": True,
            "ispublic": True,
            "ostype": "CentOS 5.6 (64-bit)",
            "mode": 'HTTP_DOWNLOAD',
        },
        "isfeatured": True,
        "ispublic": True,
        "isextractable": True,
        "bootable": True, 
        "passwordenabled": True,
        
        "template": {
            "displaytext": "xs",
            "name": "xs",
            "passwordenabled": False,
        },
        "template_2": {
            "displaytext": "Public Template",
            "name": "Public template",
            "ostype": "CentOS 5.6 (64-bit)",
            "isfeatured": True,
            "ispublic": True,
            "isextractable": True,
            "mode": "HTTP_DOWNLOAD",
        },
        "templatefilter": 'self',

        "templates": {
           "displaytext": 'Template',
           "name": 'Template',
           "ostype": "CentOS 5.3 (64-bit)",
           "templatefilter": 'self',
        },
        "security_group" : { "name": "custom_Sec_Grp" },
        "ingress_rule": {
            "protocol": "TCP",
            "startport": "22",
            "endport": "22",
            "cidrlist": "0.0.0.0/0"
        },
        "ostype": "CentOS 5.6 (64-bit)",
        "sleep": 90,
        "timeout": 10,
        "page": 1,
        "pagesize": 2,
        "listall":'true',
        "advanced_sg": {
            "zone": {
                "name": "",
                "dns1": "8.8.8.8",
                "internaldns1": "192.168.100.1",
                "networktype": "Advanced",
                "securitygroupenabled": "true"
            },
            "securitygroupenabled": "true"
        },
        "portableiprange_vlan": {
                "part": ["4090-4091", "4092-4095"],
                "full": "4090-4095"
        },
        "nfs": {
            "url": "nfs://nfs/export/automation/1/testprimary",
            "name": "Primary XEN"
        },
        "iscsi": {
            "url": "iscsi://192.168.100.21/iqn.2012-01.localdomain.clo-cstack-cos6:iser/1",
            "name": "Primary iSCSI"
        },
        "volume": {"diskname": "Test Volume"},
        "custom_volume": {
                          "customdisksize": 1,
                          "diskname": "Custom disk",
                         },
        "upload_volume": {
                          "diskname": "UploadVol",
                          "format": "VHD",
                          "url": "http://10.147.28.7/templates/393d3550-05ef-330f-9b8c-745b0e699759.vhd",
                          "checksum": "",
                         },
        "recurring_snapshot": {
                               "maxsnaps": 2,
                               "timezone": "US/Arizona",
                               },
        "volume_offerings": {
            0: {"diskname": "TestDiskServ"},
        },
        "diskdevice": ['/dev/vdc',  '/dev/vdb', '/dev/hdb', '/dev/hdc', '/dev/xvdd', '/dev/cdrom', '/dev/sr0',  '/dev/cdrom1' ],
        
        #test_vpc_vpn.py
        "vpn_user": {
            "username": "test",
            "password": "password",
        },
        "vpc": {
            "name": "vpc_vpn",
            "displaytext": "vpc-vpn",
            "cidr": "10.1.1.0/24"
        },
        "ntwk": {
            "name": "tier1",
            "displaytext": "vpc-tier1",
            "gateway" : "10.1.1.1",
            "netmask" : "255.255.255.192"
        },
        "vpc2": {
            "name": "vpc2_vpn",
            "displaytext": "vpc2-vpn",
            "cidr": "10.2.1.0/24"
        },
        "ntwk2": {
            "name": "tier2",
            "displaytext": "vpc-tier2",
            "gateway" : "10.2.1.1",
            "netmask" : "255.255.255.192"
        },
        "server": {
                "displayname": "TestVM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP'
        },
        "privateport": 22,
        "publicport": 22,
        "protocol": 'TCP',
        "forvirtualnetwork": "true",
        "customdisksize": 1,
        "diskname": "Test Volume",
        "portableIpRange": {
        "gateway" : "10.223.252.195",
        "netmask" : "255.255.255.192",
        "startip" : "10.223.252.196",
        "endip"   : "10.223.252.197",
        "vlan"    : "1001"
       }
}
