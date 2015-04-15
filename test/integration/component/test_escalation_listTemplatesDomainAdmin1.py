
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Domain, Template
                             )
from marvin.lib.utils import *
from marvin.lib.common import (get_zone)


class Domain12(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(Domain12, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.testdata["templateregister"]["ostype"] = "Windows 7 (64-bit)"
        cls.testdata["templateregister"]["url"] = "http://10.147.28.7/templates/windows7.vhd"
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.cleanup = []

# Create 2 domain admin accounts

        cls.domain1 = Domain.create(
            cls.apiclient,
            cls.testdata["domain"])

        cls.domain2 = Domain.create(
            cls.apiclient,
            cls.testdata["domain"])

        cls.account1 = Account.create(
            cls.apiclient,
            cls.testdata["account"],
            admin=True,
            domainid=cls.domain1.id)

        cls.account2 = Account.create(
            cls.apiclient,
            cls.testdata["account2"],
            admin=True,
            domainid=cls.domain2.id)

# Register template under one domain admin(root/domain1->account 1)

        cls.templateregister = Template.register(
            cls.apiclient,
            cls.testdata["templateregister"],
            zoneid=cls.zone.id,
            hypervisor=cls.hypervisor,
            account=cls.account1.name,
            domainid=cls.domain1.id)

        cls.debug("Created account %s in domain %s" %
                  (cls.account1.name, cls.domain1.id))
        cls.debug("Created account %s in domain %s" %
                  (cls.account2.name, cls.domain2.id))

        cls.cleanup.append(cls.account1)
        cls.cleanup.append(cls.domain1)
        cls.cleanup.append(cls.account2)
        cls.cleanup.append(cls.domain2)

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

# test that the template register under root/domain1->account1 is not
# listed under root/domain2->account2

    def test_listtemplate(self):
        listtemplate = Template.list(
            self.apiclient,
            zoneid=self.zone.id,
            hypervisor=self.hypervisor,
            account=self.account2.name,
            domainid=self.account2.domainid,
            templatefilter=self.testdata["templatefilter"]

        )

        self.assertEqual(
            listtemplate,
            None,
            "Check templates are not listed"
        )
