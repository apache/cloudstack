# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

nuage_test_data = {
    "dns_rule": {
        "privateport": 53,
        "publicport": 53,
        "startport": 53,
        "endport": 53,
        "protocol": "UDP",
        "cidrlist": '0.0.0.0/0',
    },
    "vpc_offering_reduced": {
        "name": "VPC reduced off",
        "displaytext": "VPC reduced off",
        "supportedservices":
            "Dhcp,Dns,SourceNat,UserData,StaticNat,NetworkACL"
    },
    "shared_network_config_drive_offering": {
        "name": 'shared_network_config_drive_offering',
        "displaytext": 'shared_network_config_drive_offering',
        "guestiptype": 'shared',
        "supportedservices": 'Dhcp,UserData',
        "traffictype": 'GUEST',
        "specifyVlan": "True",
        "specifyIpRanges": "True",
        "availability": 'Optional',
        "serviceProviderList": {
            "Dhcp": "VirtualRouter",
            "UserData": 'ConfigDrive'
        }
    },
    "isolated_staticnat_network_offering": {
        "name": 'isolated_staticnat_net_off_marvin',
        "displaytext": 'isolated_staticnat_net_off_marvin',
        "guestiptype": 'Isolated',
        "supportedservices": 'Dhcp,SourceNat,StaticNat,UserData,Firewall,Dns',
        "traffictype": 'GUEST',
        "ispersistent": 'True',
        "availability": 'Optional',
        "tags": 'native',
        "serviceProviderList": {
            "Dhcp": 'VirtualRouter',
            "StaticNat": 'VirtualRouter',
            "SourceNat": 'VirtualRouter',
            "Firewall": 'VirtualRouter',
            "UserData": 'VirtualRouter',
            "Dns": 'VirtualRouter'
        }
    },
    "nw_offering_reduced_vpc": {
        "name": 'Reduced Network for VPC',
        "displaytext": 'Reduced Network for VPC',
        "guestiptype": 'Isolated',
        "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,UserData,'
                             'Dns',
        "traffictype": 'GUEST',
        "availability": 'Optional',
        "tags": "native",
        "useVpc": 'on',
        "ispersistent": 'True',
        "serviceProviderList": {
            "Dhcp": "VpcVirtualRouter",
            "StaticNat": "VpcVirtualRouter",
            "SourceNat": "VpcVirtualRouter",
            "NetworkACL": "VpcVirtualRouter",
            "UserData": "VpcVirtualRouter",
            "Dns": "VpcVirtualRouter"
        }
    },
    # Nuage VSP SDN plugin specific test data
    "nuagevsp": {
        # Services supported by the Nuage VSP plugin for Isolated networks
        "isolated_network_offering": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,UserData,Firewall,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp',
                "UserData": 'VirtualRouter',
                "Dns": 'VirtualRouter'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        # Persistent services supported by the Nuage VSP plugin for Isolated networks
        "isolated_network_offering_persistent": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,UserData,Firewall,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "ispersistent": 'True',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp',
                "UserData": 'VirtualRouter',
                "Dns": 'VirtualRouter'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        # Persistent services supported by the Nuage VSP plugin for Isolated networks
        "isolated_network_offering_persistent": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,UserData,Firewall,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "ispersistent": 'True',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp',
                "UserData": 'VirtualRouter',
                "Dns": 'VirtualRouter'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        # Purely nuage network offering
        "isolated_network_offering_without_vr": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,Firewall',
            "traffictype": 'GUEST',
            "availabiliy": 'Optional',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        # Purely persistent nuage network offering
        "isolated_network_offering_without_vr_persistent": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,Firewall',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "tags": "nuage",
            "ispersistent": 'True',
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        # Services supported by the Nuage VSP plugin for VPC networks
        "vpc_network_offering": {
            "name": 'nuage_vpc_marvin',
            "displaytext": 'nuage_vpc_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,UserData,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "useVpc": 'on',
            "ispersistent": 'True',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "VpcVirtualRouter",
                "Dns": "VpcVirtualRouter"
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        "vpc_network_offering_internal_lb": {
            "name": "nuage_vpc_marvin_internal_lb",
            "displaytext": "nuage_vpc_marvin_internal_lb",
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,Lb,StaticNat,SourceNat,NetworkACL,Connectivity,UserData,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "tags": "nuage",
            "useVpc": 'on',
            "ispersistent": 'True',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "Lb": "InternalLbVm",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "VpcVirtualRouter",
                "Dns": "VpcVirtualRouter"
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"},
                "Lb": {"lbSchemes": "internal", "SupportedLbIsolation": "dedicated"}
            }
        },
        # Services supported by the Nuage VSP plugin for VPCs
        "vpc_offering": {
            "name": 'Nuage VSP VPC offering',
            "displaytext": 'Nuage VSP VPC offering',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,UserData,Dns',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "VpcVirtualRouter",
                "Dns": "VpcVirtualRouter"
            }
        },
        "vpc_offering_lb": {
            "name": 'Nuage VSP VPC offering with Lb',
            "displaytext": 'Nuage VSP VPC offering with Lb',
            "supportedservices": 'Dhcp,Lb,StaticNat,SourceNat,NetworkACL,Connectivity,UserData,Dns',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "Lb": "InternalLbVm",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "VpcVirtualRouter",
                "Dns": "VpcVirtualRouter"
            }
        },
        # Services supported by the Nuage VSP plugin for VPC without userdata
        "vpc_network_offering_nuage_dhcp": {
            "name": 'nuage_vpc_marvin',
            "displaytext": 'nuage_vpc_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "useVpc": 'on',
            "ispersistent": 'True',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "Dns": "VpcVirtualRouter",
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        "isolated_configdrive_network_offering_withoutdns" : {
            "name": 'nuage_configdrive_withoutDns_marvin',
            "displaytext": 'nuage_configdrive_withoutDns_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,UserData,Firewall',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp',
                "UserData": 'ConfigDrive'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        "isolated_configdrive_network_offering": {
            "name": 'nuage_configdrive_marvin',
            "displaytext": 'nuage_configdrive_marvin',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,SourceNat,Connectivity,StaticNat,UserData,Firewall,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "serviceProviderList": {
                "Dhcp": 'NuageVsp',
                "StaticNat": 'NuageVsp',
                "SourceNat": 'NuageVsp',
                "Firewall": 'NuageVsp',
                "Connectivity": 'NuageVsp',
                "UserData": 'ConfigDrive',
                "Dns": 'VirtualRouter'
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        "vpc_network_offering_configdrive_withoutdns" : {
            "name": 'nuage_vpc_marvin_configdrive_withoutdns',
            "displaytext": 'nuage_vpc_marvin_configdrive_withoutdns',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,UserData',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "useVpc": 'on',
            "ispersistent": 'True',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "ConfigDrive"
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        "vpc_network_offering_configdrive_withdns" : {
            "name": 'nuage_vpc_marvin_configdrive_withdns',
            "displaytext": 'nuage_vpc_marvin_configdrive_withdns',
            "guestiptype": 'Isolated',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,UserData,Dns',
            "traffictype": 'GUEST',
            "availability": 'Optional',
            "useVpc": 'on',
            "ispersistent": 'True',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "ConfigDrive",
                "Dns": "VpcVirtualRouter"
            },
            "serviceCapabilityList": {
                "SourceNat": {"SupportedSourceNatTypes": "perzone"}
            }
        },
        "vpc_offering_configdrive_withoutdns" : {
            "name": 'Nuage VSP VPC offering ConfigDrive',
            "displaytext": 'Nuage VSP VPC offering ConfigDrive',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,UserData',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "ConfigDrive"
            }
        },
        "vpc_offering_configdrive_withdns" :{
            "name": 'Nuage VSP VPC offering ConfigDrive withVR',
            "displaytext": 'Nuage VSP VPC offering ConfigDrive withVR',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,UserData,Dns',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": "ConfigDrive",
                "Dns": "VpcVirtualRouter"
            }
        },
        "shared_nuage_network_config_drive_offering" : {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'shared',
            "supportedservices": 'Dhcp,Connectivity,UserData',
            "traffictype": 'GUEST',
            "specifyVlan": "False",
            "specifyIpRanges": "True",
            "availability": 'Optional',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "Connectivity": "NuageVsp",
                "UserData": 'ConfigDrive'
            },
            "serviceCapabilityList": {
                "Connectivity": {
                    "PublicAccess": "true"
                }
            }
        },
        "network_all2" : {
            "name": "SharedNetwork2-All-nuage",
            "displaytext": "SharedNetwork2-All-nuage",
            "gateway": "10.200.200.1",
            "netmask": "255.255.255.0",
            "startip": "10.200.200.21",
            "endip": "10.200.200.100",
            "acltype": "Domain"
        },
        # Services supported by the Nuage VSP plugin for VPCs
        "vpc_offering_nuage_dhcp": {
            "name": 'Nuage VSP VPC offering',
            "displaytext": 'Nuage VSP VPC offering',
            "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,Connectivity,Dns',
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "StaticNat": "NuageVsp",
                "SourceNat": "NuageVsp",
                "NetworkACL": "NuageVsp",
                "Connectivity": "NuageVsp",
                "Dns": "VpcVirtualRouter",
            }
        },
        "shared_nuage_network_offering": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'shared',
            "supportedservices": 'Dhcp,Connectivity',
            "traffictype": 'GUEST',
            "specifyVlan": "False",
            "specifyIpRanges": "True",
            "availability": 'Optional',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "Connectivity": "NuageVsp"
            }
        },
        "shared_nuage_public_network_offering": {
            "name": 'nuage_marvin',
            "displaytext": 'nuage_marvin',
            "guestiptype": 'shared',
            "supportedservices": 'Dhcp,Connectivity',
            "traffictype": 'GUEST',
            "specifyVlan": "False",
            "specifyIpRanges": "True",
            "availability": 'Optional',
            "tags": "nuage",
            "serviceProviderList": {
                "Dhcp": "NuageVsp",
                "Connectivity": "NuageVsp"
            },
            "serviceCapabilityList": {
                "Connectivity": {
                    "PublicAccess": "true"
                }
            }

        },
        # Test data for Shared Network creation
        "network_all": {
            "name": "SharedNetwork-All-nuage",
            "displaytext": "SharedNetwork-All-nuage",
            "gateway": "10.200.100.1",
            "netmask": "255.255.255.0",
            "startip": "10.200.100.21",
            "endip": "10.200.100.100",
            "acltype": "Domain"
        },
        "network_domain_with_no_subdomain_access": {
            "name": "SharedNetwork-Domain-nosubdomain-nuage",
            "displaytext": "SharedNetwork-Domain-nosubdomain-nuage",
            "gateway": "10.222.1.1",
            "netmask": "255.255.255.0",
            "startip": "10.222.1.2",
            "endip": "10.222.1.100",
            "acltype": "Domain",
            "subdomainaccess": "false"
        },
        "network_domain_with_subdomain_access": {
            "name": "SharedNetwork-Domain-withsubdomain-nuage",
            "displaytext": "SharedNetwork-Domain-withsubdomain-nuage",
            "gateway": "10.221.1.1",
            "netmask": "255.255.255.0",
            "startip": "10.221.1.2",
            "endip": "10.221.1.100",
            "acltype": "Domain",
            "subdomainaccess": "true"
        },
        "network_account": {
            "name": "SharedNetwork-Account-nuage",
            "displaytext": "SharedNetwork-Account-nuage",
            "gateway": "10.220.1.1",
            "netmask": "255.255.255.0",
            "startip": "10.220.1.2",
            "endip": "10.220.1.100",
            "acltype": "Account"
        },
        "shared_network_offering": {
            "name": "MySharedOffering-shared",
            "displaytext": "MySharedOffering",
            "guestiptype": "Shared",
            "supportedservices": "Dhcp,Dns,UserData",
            "specifyVlan": "True",
            "specifyIpRanges": "True",
            "traffictype": "GUEST",
            "tags": "native",
            "serviceProviderList": {
                "Dhcp": "VirtualRouter",
                "Dns": "VirtualRouter",
                "UserData": "VirtualRouter"
            }
        },
        "publiciprange1": {
            "gateway": "10.200.100.1",
            "netmask": "255.255.255.0",
            "startip": "10.200.100.101",
            "endip": "10.200.100.105",
            "forvirtualnetwork": "false"
        },
        "publiciprange2": {
            "gateway": "10.219.1.1",
            "netmask": "255.255.255.0",
            "startip": "10.219.1.2",
            "endip": "10.219.1.5",
            "forvirtualnetwork": "false"
        },
        "publiciprange3": {
            "gateway": "10.200.100.1",
            "netmask": "255.255.255.0",
            "startip": "10.200.100.2",
            "endip": "10.200.100.20",
            "forvirtualnetwork": "false"
        }
    }
}
