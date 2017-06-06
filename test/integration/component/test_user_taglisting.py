# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             validateList,
                             Tag
                            )
from marvin.lib.common import (get_domain,
                                get_zone,
                                get_template)
from marvin.lib.utils import cleanup_resources
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
        cls.api_client = cls.testClient.getUserApiClient(UserName=cls.account.name, DomainName=cls.account.domain)

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
    @attr(tags=["advanced","sg"])
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
            zoneid=self.zone.id
        )

        tag1 = Tag.create(
            self.apiclient,
            resourceIds=self.virtual_machine.id,
            resourceType='userVM',
            tags={'vmtag'+self.virtual_machine.id: 'autotag'+self.virtual_machine.id}
        )

        tags = Tag.list(
            self.apiclient,
            listall=True,
            resourceType='userVM',
            key='vmtag'+self.virtual_machine.id,
            value='autotag'+self.virtual_machine.id
        )


        self.assertEqual(
                           validateList(tags)[0],
                            PASS,
                            "There is just one tag listed with list tags"
                        )

        self.assertLess(
                            len(tags),
                            2,
                            "The user tag is listed multiple times"
                        )
        return



