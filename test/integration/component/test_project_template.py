from marvin.codes import FAILED, PASS
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import Account, VirtualMachine, ServiceOffering,Project,Network,NetworkOffering,Domain,Volume,Template
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr



class TestProjectPrivateTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVM, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.cleanup = []

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

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

        cls.new_domain = Domain.create(
                                   cls.apiclient,
                                   cls.services["domain"],
                                   )
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.new_domain.id
                            )

        cls.debug(cls.account.id)
        #Fetch an api client with the user account created

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def test_project_private_template(self):
        self.api_client = self.testClient.getUserApiClient(UserName=self.account.name, DomainName=self.account.domain)

        #Create a new project using the account created
        self.project = Project.create(self.api_client,self.services["project"],account=self.account.name,domainid=self.new_domain.id)
        self.debug("The project has been created")

        self.network_offering = NetworkOffering.create(
            self.apiclient,
            self.services["network_offering"],
        )
        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

        self.services["network"]["networkoffering"] = self.network_offering.id



        #cls.api_client = testClient.getUserApiClient(UserName=cls.account.name, DomainName=cls.account.domain)

        self.project_network = Network.create(
            self.api_client,
            self.services["network"],
            networkofferingid=self.network_offering.id,
            zoneid=self.zone.id,
            projectid=self.project.id
        )


        self.virtual_machine = VirtualMachine.create(
            self.api_client,
            self.services["small"],
            serviceofferingid=self.service_offering.id,
            projectid=self.project.id,
            networkids=self.project_network.id,
            zoneid=self.zone.id
        )

        #Stop virtual machine
        self.virtual_machine.stop(self.api_client)

        list_volumes = Volume.list(
                                   self.api_client,
                                   projectid=self.project.id,
                                   virtualmachineid=self.virtual_machine.id,
                                   type='ROOT',
                                   listall=True
                                   )

        self.volume = list_volumes[0]

        #Create template from Virtual machine and Volume ID
        self.services["project"]["ispublic"] = False
        self.services["project"]["displaytext"] = "This template should be visible only in project scope"
        self.services["project"]["ostype"] = self.services["ostype"]
        self.services["project"]["templatefilter"] = self.services["templatefilter"]

        project_template = Template.create(
                                self.api_client,
                                self.services["project"],
                                self.volume.id,
                                projectid=self.project.id
                                )
        self.debug("Created template with ID: %s" % project_template.id)

        #api_client = cls.testClient.getUserApiClient(UserName=cls.account.name, DomainName=cls.account.domain)

        list_template_response = Template.list(
                                    self.api_client,
                                    templatefilter=self.services["project"]["templatefilter"],
                                    projectid=self.project.id
                                    )

        self.cleanup.append(self.project)
        self.cleanup.append(self.account)
        self.cleanup.append(self.service_offering)
        self.cleanup.append(self.network_offering)
        self.cleanup.append(self.new_domain)

        self.assertEqual(
                           len(list_template_response),
                            1,
                            "There is one private template for this account in this project"
                        )




    

