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
""" BVT tests for Bucket Operations"""

#Import Local Modules
from marvin.cloudstackTestCase import *
from nose.plugins.attrib import attr
from marvin.lib.base import (ObjectStoragePool, Bucket)
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
    def test_01_create_bucket(self):
        """Test to create bucket in object store

        """

        object_store = ObjectStoragePool.create(
            self.apiclient,
            "testOS-9",
            "http://192.168.0.1",
            "Simulator",
            None
        )

        self.debug("Created Object Store with ID: %s" % object_store.id)

        bucket = Bucket.create(
            self.apiclient,
            "mybucket",
            object_store.id,
            quota=1
        )

        list_buckets_response = Bucket.list(
            self.apiclient,
            id=bucket.id
        )

        self.assertNotEqual(
            len(list_buckets_response),
            0,
            "Check List Bucket response"
        )

        bucket_response = list_buckets_response[0]
        self.assertEqual(
            object_store.id,
            bucket_response.objectstorageid,
            "Check object store id of the created Bucket"
        )
        self.assertEqual(
            "mybucket",
            bucket_response.name,
            "Check Name of the created Bucket"
        )

        bucket.update(
            self.apiclient,
            quota=100
        )

        list_buckets_response_updated = Bucket.list(
            self.apiclient,
            id=bucket.id
        )

        bucket_response_updated = list_buckets_response_updated[0]

        self.assertEqual(
            100,
            bucket_response_updated.quota,
            "Check quota of the updated bucket"
        )

        self.cleanup.append(bucket)
        self.cleanup.append(object_store)

    @attr(tags=["smoke"], required_hardware="false")
    def test_02_bucket_key_rotation(self):
        """Test per-bucket credentials and two-slot key rotation on the simulator object store

        The simulator provider supports dedicated bucket credentials, so a new bucket gets
        its own credential with a key in slot 1. Rotating fills slot 2 with a different key,
        revoking slot 1 leaves slot 2 active, and the last active key cannot be revoked.
        """

        object_store = ObjectStoragePool.create(
            self.apiclient,
            "testOS-keys",
            "http://192.168.0.2",
            "Simulator",
            None
        )
        bucket = Bucket.create(
            self.apiclient,
            "rotatebucket",
            object_store.id,
            quota=1
        )
        self.cleanup.append(bucket)
        self.cleanup.append(object_store)

        bucket_response = Bucket.list(self.apiclient, id=bucket.id)[0]
        self.assertEqual("bucket", bucket_response.credentialscope, "New bucket should have a dedicated credential")
        self.assertEqual(1, len(bucket_response.keys), "New bucket should have one key slot")
        slot1 = bucket_response.keys[0]
        self.assertEqual(1, slot1.keyslot)
        self.assertEqual("Active", slot1.state)
        self.assertEqual(slot1.accesskey, bucket_response.accesskey, "Bucket access key should mirror the active key")

        rotated = bucket.rotate_key(self.apiclient)
        self.assertEqual(2, rotated.keyslot, "Default rotation should fill the free slot")
        self.assertNotEqual(slot1.accesskey, rotated.accesskey, "Rotated key must differ from the existing key")

        bucket_response = Bucket.list(self.apiclient, id=bucket.id)[0]
        self.assertEqual(2, len(bucket_response.keys), "Both slots should be populated after rotation")
        self.assertEqual(rotated.accesskey, bucket_response.accesskey, "Bucket access key should follow the newest active key")

        bucket.revoke_key(self.apiclient, keyslot=1)
        bucket_response = Bucket.list(self.apiclient, id=bucket.id)[0]
        states = {key.keyslot: key.state for key in bucket_response.keys}
        self.assertEqual("Revoked", states[1], "Slot 1 should be revoked")
        self.assertEqual("Active", states[2], "Slot 2 should stay active")

        with self.assertRaises(Exception):
            bucket.revoke_key(self.apiclient, keyslot=2)

        rotated_again = bucket.rotate_key(self.apiclient)
        self.assertEqual(1, rotated_again.keyslot, "Rotation should reuse the revoked slot")
        bucket_response = Bucket.list(self.apiclient, id=bucket.id)[0]
        states = {key.keyslot: key.state for key in bucket_response.keys}
        self.assertEqual({1: "Active", 2: "Active"}, states, "Both slots should be active again")

        return
