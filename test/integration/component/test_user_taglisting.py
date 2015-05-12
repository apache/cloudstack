from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             NetworkOffering,
                             Network,
                             Tag
                            )
from marvin.lib.common import (get_domain,
                                get_zone,
                                get_template)
from marvin.cloudstackAPI import *
from nose.plugins.attrib import attr
from marvin.codes import FAILED, PASS

class TestDeployVMWithTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.cleanup = []
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.cleanup.append(cls.account)
        template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )
        if template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id

        cls.services["medium"]["zoneid"] = cls.zone.id
        cls.services["medium"]["template"] = template.id
        cls.services["iso1"]["zoneid"] = cls.zone.id

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.cleanup.append(cls.service_offering)
        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
        )
        cls.cleanup.append(cls.network_offering)
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls.services["network"]["networkoffering"] = cls.network_offering.id
        cls.api_client = cls.testClient.getUserApiClient(UserName=cls.account.name, DomainName=cls.account.domain)
        cls.network = Network.create(
            cls.api_client,
            cls.services["network"],
            networkofferingid=cls.network_offering.id,
            accountid=cls.account.name,
            domainid=cls.domain.id,
            zoneid=cls.zone.id
        )
        cls.cleanup.append(cls.network)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def test_deploy_vm_with_tags(self):
        """Test Deploy Virtual Machine
        """
        # Validate the following:
        # 1. User tags are created for VM
        # 2. listing the tags for the VM will list the tag only once and doesn't list the same tag multiple times
        self.virtual_machine = VirtualMachine.create(
            self.api_client,
            self.services["small"],
            serviceofferingid=self.service_offering.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            networkids=self.network.id,
            zoneid=self.zone.id
        )

        tag1 = Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine.id,
            resourceType='userVM',
            tags={'vmtag': 'autotag'}
        )
        tag2 = Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine.id,
            resourceType='userVM',
            tags={'vmtag1': 'autotag'}
        )

        tags = Tag.list(
            self.apiclient,
            listall=True,
            resourceType='userVM',
            key='vmtag',
            value='autotag'
        )

        self.assertEqual(
                           len(tags),
                            1,
                            "There is just one tag listed with list tags"
                        )

        self.assertGreater(
                            len(tags),
                            1,
                            "The user tag is listed multiple times"
                        )





