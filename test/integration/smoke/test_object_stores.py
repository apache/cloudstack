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
""" BVT tests for Object Storage Pool"""

#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from nose.plugins.attrib import attr
from marvin.lib.base import (ObjectStoragePool)
from marvin.lib.utils import (cleanup_resources)

_multiprocess_shared_ = True

class TestObjectStore(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created resources
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["smoke"], required_hardware="false")
    def test_01_create_object_store(self):
        """Test to create object store
        """

        object_store = ObjectStoragePool.create(
            self.apiclient,
            "testOS-10",
            "http://192.168.0.1",
            "Simulator",
            None
        )

        self.debug("Created Object Store with ID: %s" % object_store.id)

        list_object_stores_response = ObjectStoragePool.list(
            self.apiclient,
            id=object_store.id
        )

        self.assertNotEqual(
            len(list_object_stores_response),
            0,
            "Check List Object Store response"
        )

        object_store_response = list_object_stores_response[0]
        self.assertEqual(
            "Simulator",
            object_store_response.providername,
            "Check Provider of the created Object Store"
        )
        self.assertEqual(
            "testOS-10",
            object_store_response.name,
            "Check Name of the created Object Store"
        )
        self.assertEqual(
            "http://192.168.0.1",
            object_store_response.url,
            "Check URL of the created Object Store"
        )

        object_store.update(
            self.apiclient,
            name="updated_name"
        )

        list_object_stores_response_updated = ObjectStoragePool.list(
            self.apiclient,
            id=object_store.id
        )

        object_store_response_updated = list_object_stores_response_updated[0]

        self.assertEqual(
            "updated_name",
            object_store_response_updated.name,
            "Check Name of the updated Object Store name"
        )

        self.cleanup.append(object_store)

        return
