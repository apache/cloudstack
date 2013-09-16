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
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
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
    """Test Regions - CRUD tests for regions
    """

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestRegions, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.cleanup = []
        return

    def setUp(self):
        pseudo_random_int = choice(xrange(1, 200))
        self.services["region"]["regionid"] = pseudo_random_int
        self.services["region"]["regionname"] = "region" + str(pseudo_random_int)
        self.services["region"]["regionendpoint"] = "http://region" + str(pseudo_random_int) + ":8080/client"

        self.region = Region.create(self.api_client,
            self.services["region"]
        )
        self.cleanup = []
        self.cleanup.append(self.region)

        list_region = Region.list(self.api_client,
            id=self.services["region"]["regionid"]
        )

        self.assertEqual(
            isinstance(list_region, list),
            True,
            msg="Region creation failed"
        )

    @attr(tags=["simulator", "basic", "advanced"])
    def test_createRegionWithExistingRegionId(self):
        """Test for duplicate checks on region id
        """
        self.services["region"]["regionname"] = random_gen() #alter region name but not id
        self.assertRaises(Exception, Region.create, self.api_client, self.services["region"])

    @attr(tags=["simulator", "basic", "advanced"])
    def test_createRegionWithExistingRegionName(self):
        """Test for duplicate checks on region name
        """
        random_int = choice(xrange(1, 200))
        self.services["region"]["regionid"] = random_int  #alter id but not name
        self.services["region"]["regionendpoint"] = "http://region" + str(random_int) + ":8080/client"
        self.assertRaises(Exception, Region.create, self.api_client, self.services["region"])

    @attr(tags=["simulator", "basic", "advanced"])
    def test_updateRegion(self):
       """ Test for update Region
       """
       self.services["region"]["regionname"] = "Region3" + random_gen()
       self.services["region"]["regionendpoint"] = "http://region3updated:8080/client"

       updated_region = self.region.update(self.api_client,
           self.services["region"]
       )

       list_region = Region.list(self.api_client,
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
            cleanup_resources(self.api_client, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @classmethod
    def tearDownClass(cls):
        """
        Nothing to do
        """
        pass
