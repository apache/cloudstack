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

from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from random import choice

class Services:
    def __init__(self):
        self.services = {
            "region": {
                "regionid": "2",
                "regionname": "Region2",
                "regionendpoint": "http://region2:8080/client"
            }
        }

class TestRegions(cloudstackTestCase):
    """Test Regions - basic region creation
    """
    def setUp(self):
        testClient = super(TestRegions, self).getClsTestClient()
        self.apiclient = testClient.getApiClient()
        self.services = testClient.getParsedTestDataConfig()

        self.domain = get_domain(self.apiclient)
        self.cleanup = []
        pseudo_random_int = choice(range(2, 200))
        self.services["region"]["regionid"] = pseudo_random_int
        self.services["region"]["regionname"] = "region" + str(pseudo_random_int)
        self.services["region"]["regionendpoint"] = "http://region" + str(pseudo_random_int) + ":8080/client"

        self.region = Region.create(self.apiclient,
            self.services["region"]
        )
        self.cleanup = []
        self.cleanup.append(self.region)

        list_region = Region.list(self.apiclient,
            id=self.services["region"]["regionid"]
        )

        self.assertEqual(
            isinstance(list_region, list),
            True,
            msg="Region creation failed"
        )

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_createRegion(self):
        """ Test for create region
        """
        list_region = Region.list(self.apiclient,
            id=self.services["region"]["regionid"]
        )

        self.assertEqual(
            isinstance(list_region, list),
            True,
            "Check for list Region response"
        )
        region_response = list_region[0]
        id = self.services["region"]["regionid"]
        self.assertEqual(
            region_response.id,
            self.services["region"]["regionid"],
            "listRegion response does not match with region Id created"
        )

        self.assertEqual(
            region_response.name,
            self.services["region"]["regionname"],
            "listRegion response does not match with region name created"
        )
        self.assertEqual(
            region_response.endpoint,
            self.services["region"]["regionendpoint"],
            "listRegion response does not match with region endpoint created"
        )

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_createRegionWithExistingRegionId(self):
        """Test for duplicate checks on region id
        """
        self.services["region"]["regionname"] = random_gen()  # alter region name but not id
        self.assertRaises(Exception, Region.create, self.apiclient, self.services["region"])

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_createRegionWithExistingRegionName(self):
        """Test for duplicate checks on region name
        """
        random_int = choice(range(2, 200))
        self.services["region"]["regionid"] = random_int  # alter id but not name
        self.services["region"]["regionendpoint"] = "http://region" + str(random_int) + ":8080/client"
        self.assertRaises(Exception, Region.create, self.apiclient, self.services["region"])

    @attr(tags=["simulator", "basic", "advanced"], required_hardware="false")
    def test_updateRegion(self):
        """ Test for update Region
        """
        self.services["region"]["regionname"] = "Region3" + random_gen()
        self.services["region"]["regionendpoint"] = "http://region3updated:8080/client"

        updated_region = self.region.update(self.apiclient,
                                            self.services["region"]
                                            )

        list_region = Region.list(self.apiclient,
                                  id=self.services["region"]["regionid"]
                                  )

        self.assertEqual(
            isinstance(list_region, list),
            True,
            "Check for list Region response"
        )
        region_response = list_region[0]

        self.assertEqual(
            region_response.id,
            updated_region.id,
            "listRegion response does not match with region Id created"
        )

        self.assertEqual(
            region_response.name,
            updated_region.name,
            "listRegion response does not match with region name created"
        )
        self.assertEqual(
            region_response.endpoint,
            updated_region.endpoint,
            "listRegion response does not match with region endpoint created"
        )

    def tearDown(self):
        """ Test for delete region as cleanup
        """
        try:
            #Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
