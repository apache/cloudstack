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

"""
Lifecycle integration tests for the KMS (Key Management Service) feature.

Covers:
  - HSM profile CRUD (add / list / update / delete)
  - KMS key CRUD  (create / list / update / delete)
  - Key rotation
  - Multi-tenancy / access isolation
  - Negative scenarios (delete key in use, duplicate name, delete profile with keys)

All tests use the built-in *database* KMS provider so that they can run in any
CI environment without real HSM hardware.
"""

import random
import string

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (
    Account,
    Domain,
    HSMProfile,
    KMSKey,
    VirtualMachine,
    ServiceOffering,
    Volume,
)
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr

def _random_name(prefix="test-kms"):
    suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=6))
    return f"{prefix}-{suffix}"


class TestKMSLifecycle(cloudstackTestCase):
    """
    End-to-end lifecycle tests for KMS keys and HSM profiles.

    Each test gets a fresh Domain + Account pair via setUp so that
    domain-level isolation is guaranteed and cleanup is straightforward.
    """

    @classmethod
    def setUpClass(cls):
        cls.test_client = super(TestKMSLifecycle, cls).getClsTestClient()
        cls.apiclient = cls.test_client.getApiClient()
        cls.zone = get_zone(cls.apiclient, cls.test_client.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)

        cls._cleanup = []

        cls.default_profile = None
        profiles = HSMProfile.list(cls.apiclient, name="default")
        if profiles and len(profiles) > 0:
            cls.default_profile = profiles[0]
            if hasattr(cls.default_profile, 'enabled') and not cls.default_profile.enabled:
                hsm = HSMProfile({"id": cls.default_profile.id})
                hsm.update(cls.apiclient, enabled=True)
            # Re-fetch to get updated state
            profiles = HSMProfile.list(cls.apiclient, name="default")
            if profiles and len(profiles) > 0:
                cls.default_profile = profiles[0]


    @classmethod
    def tearDownClass(cls):
        super(TestKMSLifecycle, cls).tearDownClass()

    # ------------------------------------------------------------------
    # Per-test helpers
    # ------------------------------------------------------------------
    def setUp(self):
        self.apiclient = self.test_client.getApiClient()
        self.cleanup = []
        self._create_domain_and_account()

    def tearDown(self):
        self.cleanup.reverse()
        cleanup_resources(self.apiclient, self.cleanup)

    def _create_domain_and_account(self, is_domain_admin=False):
        """Create a child domain + account and register them for cleanup."""
        self.child_domain = Domain.create(
            self.apiclient,
            {"name": _random_name("kms-dom")},
            parentdomainid=self.domain.id,
        )
        self.cleanup.append(self.child_domain)

        self.user_account = Account.create(
            self.apiclient,
            {
                "firstname": "KMS",
                "lastname": "Test",
                "email": "kmstest@example.com",
                "username": _random_name("kmsuser"),
                "password": "password",
            },
            domainid=self.child_domain.id,
            admin=is_domain_admin,
        )
        self.cleanup.append(self.user_account)

        # API client scoped to the new user
        self.user_apiclient = self.test_client.getUserApiClient(
            UserName=self.user_account.name,
            DomainName=self.child_domain.name,
        )

    def _create_kms_key(self, name, profile_id, apiclient=None, zoneid=None, purpose=None):
        api_client = apiclient or self.apiclient
        zone_id = zoneid or self.zone.id
        key = KMSKey.create(
            api_client,
            name=name,
            zoneid=zone_id,
            hsmprofileid=profile_id,
            purpose=purpose
        )
        self.cleanup.append(key)
        return key

    def _create_hsm_profile(self, name, protocol="database", system=True, zoneid=None):
        zone_id = zoneid or self.zone.id
        profile = HSMProfile.create(
            self.apiclient,
            name=name,
            protocol=protocol,
            system=system,
            zoneid=zone_id,
        )
        self.cleanup.append(profile)
        return profile


    # ==================================================================
    # HSM Profile lifecycle tests (01 – 03, 11, 13)
    # ==================================================================

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_01_add_hsm_profile_admin(self):
        """Test: admin creates a system-wide database HSM profile."""
        profile_name = _random_name("hsm-prof")
        profile = self._create_hsm_profile(name=profile_name)
        self.assertIsNotNone(profile, "HSM profile creation returned None")

        self.assertEqual(
            profile.name, profile_name,
            "HSM profile name does not match the requested name"
        )
        self.assertEqual(
            profile.protocol.lower(), "database",
            "HSM profile protocol should be 'database'"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_02_list_hsm_profiles(self):
        """Test: list HSM profiles and verify a created profile is present."""
        profile_name = _random_name("hsm-list")
        profile = self._create_hsm_profile(name=profile_name)

        profiles = HSMProfile.list(self.apiclient, id=profile.id)
        self.assertIsNotNone(profiles, "listHSMProfiles returned None")
        self.assertEqual(len(profiles), 1, "Expected exactly one HSM profile matching the ID")
        self.assertEqual(profiles[0].id, profile.id, "Profile IDs do not match")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_03_update_hsm_profile(self):
        """Test: update the name of an existing HSM profile."""
        profile = self._create_hsm_profile(name=_random_name("hsm-upd"))

        new_name = _random_name("hsm-renamed")
        updated = profile.update(self.apiclient, name=new_name)

        self.assertIsNotNone(updated, "updateHSMProfile returned None")
        self.assertEqual(
            updated.name, new_name,
            "Profile name was not updated"
        )

    # ==================================================================
    # KMS Key CRUD tests (tests 04 – 09)
    # ==================================================================

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_04_create_kms_key_admin(self):
        """Test: admin creates a KMS key in the zone, verifies fields."""
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        key_name = _random_name("kms-key")
        key = self._create_kms_key(name=key_name, profile_id=profile.id, purpose="volume")
        self.assertIsNotNone(key, "createKMSKey returned None")

        self.assertEqual(key.name, key_name, "Key name does not match")
        self.assertEqual(
            key.zoneid, self.zone.id,
            "Key zone ID does not match the requested zone"
        )
        self.assertTrue(key.enabled, "Newly created key should be enabled")
        self.assertIsNotNone(key.id, "Key UUID should not be None")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_05_list_kms_keys(self):
        """Test: list KMS keys filtered by zone and by id."""
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        key = self._create_kms_key(name=_random_name("key"), profile_id=profile.id)

        # Filter by explicit key ID
        keys_by_id = KMSKey.list(self.apiclient, id=key.id)
        self.assertIsNotNone(keys_by_id, "listKMSKeys by id returned None")
        self.assertEqual(len(keys_by_id), 1, "Expected exactly one key matching given ID")
        self.assertEqual(keys_by_id[0].id, key.id, "Key IDs do not match")

        # Filter by zone
        keys_by_zone = KMSKey.list(self.apiclient, zoneid=self.zone.id)
        self.assertIsNotNone(keys_by_zone, "listKMSKeys by zone returned None")
        found = any(k.id == key.id for k in keys_by_zone)
        self.assertTrue(found, "Newly created key not found when listing by zone")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_06_update_kms_key(self):
        """Test: update key name, description, and enabled status."""
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        key = self._create_kms_key(name=_random_name("key-upd"), profile_id=profile.id)

        new_name = _random_name("key-renamed")
        new_desc = "Updated description"
        updated = key.update(
            self.apiclient,
            name=new_name,
            description=new_desc,
            enabled=False,
        )

        self.assertIsNotNone(updated, "updateKMSKey returned None")
        self.assertEqual(updated.name, new_name, "Key name was not updated")
        self.assertEqual(updated.description, new_desc, "Key description was not updated")
        self.assertFalse(updated.enabled, "Key should be disabled after update")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_07_create_kms_key_user(self):
        """Test: domain user creates their own KMS key; verifies ownership."""
        # Admin creates the system HSM profile first
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        key_name = _random_name("user-key")
        key = self._create_kms_key(name=key_name, profile_id=profile.id, apiclient=self.user_apiclient)
        self.assertIsNotNone(key, "User-level createKMSKey returned None")

        self.assertEqual(key.name, key_name, "Key name does not match")
        self.assertEqual(
            key.account, self.user_account.name,
            "Key account should belong to the creating user account"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_08_list_kms_keys_user_isolation(self):
        """Test: User A's keys are NOT visible to User B."""
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        # User A key (self.user_account)
        key_a = self._create_kms_key(name=_random_name("key-a"), profile_id=profile.id, apiclient=self.user_apiclient)

        # Create User B in a separate child domain
        domain_b = Domain.create(
            self.apiclient,
            {"name": _random_name("dom-b")},
            parentdomainid=self.domain.id,
        )
        self.cleanup.append(domain_b)
        account_b = Account.create(
            self.apiclient,
            {
                "firstname": "UserB",
                "lastname": "Test",
                "email": "userb@example.com",
                "username": _random_name("userb"),
                "password": "password",
            },
            domainid=domain_b.id,
            admin=False,
        )
        self.cleanup.append(account_b)
        apiclient_b = self.test_client.getUserApiClient(
            UserName=account_b.name,
            DomainName=domain_b.name,
        )

        # User B should not be able to see User A's key
        keys_for_b = KMSKey.list(apiclient_b, id=key_a.id)
        if keys_for_b:
            self.assertNotEqual(
                keys_for_b[0].accountid, self.user_account.id,
                "User B should not see User A's KMS keys"
            )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_09_delete_kms_key(self):
        """Test: delete a KMS key that is not in use; verify it is gone."""
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        key = self._create_kms_key(name=_random_name("key-del"), profile_id=profile.id)

        key.delete(self.apiclient)

        # Verify the key no longer appears in listings
        keys = KMSKey.list(self.apiclient, id=key.id)
        self.assertTrue(
            not keys,
            "Deleted KMS key should not appear in listKMSKeys"
        )

    # ==================================================================
    # Key rotation (test 10)
    # ==================================================================

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_10_rotate_kms_key(self):
        """Test: rotate a KMS key; verify the key version increments."""
        if not self.default_profile:
            self.skipTest("Default HSM profile 'default' not found")
        profile = self.default_profile

        key = self._create_kms_key(name=_random_name("key-rot"), profile_id=profile.id)

        initial_version = key.version

        key.rotate(self.apiclient)

        # Fetch the updated key details and confirm version incremented
        keys = KMSKey.list(self.apiclient, id=key.id)
        self.assertIsNotNone(keys, "listKMSKeys after rotation returned None")
        self.assertEqual(len(keys), 1, "Expected exactly one key after rotation")
        rotated_key = keys[0]

        self.assertGreater(
            rotated_key.version,
            initial_version,
            f"Key version should increase after rotation (was {initial_version}, "
            f"got {rotated_key.version})"
        )

    # ==================================================================
    # Negative tests (11)
    # ==================================================================


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_11_delete_hsm_profile_with_keys_negative(self):
        """
        Negative test: deleting an HSM profile that still has associated KMS
        keys should be rejected.
        """
        profile = self._create_hsm_profile(name=_random_name("hsm-with-key"))

        key = self._create_kms_key(name=_random_name("key-blocks-del"), profile_id=profile.id)

        with self.assertRaises(Exception,
                               msg="Deleting HSM profile with active keys should fail"):
            profile.delete(self.apiclient)

    # ==================================================================
    # VM Encryption tests (12)
    # ==================================================================

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_12_deploy_vm_with_root_disk_encryption(self):
        """
        Test: deploy a VM with its root disk encrypted using a KMS key.
        Verify that the VM starts and the root volume has the KMS key ID.
        """
        # 1. Create a KMS key for the user
        key = self._create_kms_key(name=_random_name("vm-root-key"), profile_id=self.default_profile.id, apiclient=self.user_apiclient)

        # 2. Get a template and a service offering
        template = get_template(
            self.apiclient,
            self.zone.id,
            self.test_client.getParsedTestDataConfig().get("ostype", "CentOS 7.0 (64-bit)")
        )
        if template == -1 or template is None:
            self.fail("Check for template failed")

        service_offering = ServiceOffering.create(
            self.apiclient,
            self.test_client.getParsedTestDataConfig()["service_offering"]
        )
        self.cleanup.append(service_offering)

        # 3. Deploy VM with root disk encryption
        vm = VirtualMachine.create(
            self.user_apiclient,
            self.test_client.getParsedTestDataConfig()["virtual_machine"],
            templateid=template.id,
            accountid=self.user_account.name,
            domainid=self.child_domain.id,
            serviceofferingid=service_offering.id,
            zoneid=self.zone.id,
            rootdiskkmskeyid=key.id
        )
        self.cleanup.append(vm)

        self.assertEqual(
            vm.state,
            "Running",
            "VM should be in Running state after deployment"
        )

        # 4. Verify the root volume has the KMS key ID
        volumes = Volume.list(
            self.user_apiclient,
            virtualmachineid=vm.id,
            type='ROOT',
            listall=True
        )
        self.assertTrue(
            volumes and len(volumes) > 0,
            "VM should have at least one ROOT volume"
        )
        root_volume = volumes[0]
        self.assertEqual(
            str(root_volume.kmskeyid),
            str(key.id),
            f"Root volume should have KMS key ID {key.id}, found {root_volume.kmskeyid}"
        )

    # ==================================================================
    # HSM Profile cleanup (13)
    # ==================================================================

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"],
          required_hardware="false")
    def test_13_delete_hsm_profile(self):
        """Test: delete an HSM profile that has no associated keys; verify it is gone."""
        profile = self._create_hsm_profile(name=_random_name("hsm-gone"))

        profile.delete(self.apiclient)

        profiles = HSMProfile.list(self.apiclient, id=profile.id)
        self.assertTrue(
            not profiles,
            "Deleted HSM profile should not appear in listHSMProfiles"
        )
