#!/usr/bin/env python
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
from marvin import remoteSSHClient
from nose.plugins.attrib import attr

class Services:
    """
    """

    def __init__(self):
        self.services = {
       "domain": {
       "name": "testuuid",
       "domainUUID": "domain1"
       },
            "account": {
                "email": "test@test.com",
                "firstname": "Testuuid",
                "lastname": "Useruuid",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
       "accountUUID": "account1",
       "userUUID": "user1"
            },
            "user": {
                "email": "test@test.com",
                "firstname": "Testuuid",
                "lastname": "Useruuid",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
       "userUUID": "user2"
            },
            "ostype": 'CentOS 5.3 (64-bit)',
       "region": {
       "regionid": "2",
       "regionname": "Region2",
       "regionendpoint": "http://region2:8080/client"
       }
        }


class TestRegions(cloudstackTestCase):
    """
   Testing Regions related Apis
    """
    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestRegions, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
   cls.domain = get_domain(cls.api_client, cls.services)
   cls.cleanup = []
        return

    @attr(tags=["basic", "advanced"])
    def test_createAccountWithUUID(self):
   """
   Test for creating Account by passing id parameter

        # Validate the following
        # 1.Create an Account by passing id parameter.Verify the account is created.
        # 2.List this account by passing id parameter.Verify that list account is able to lis this account.
        # 3.Delete account should succeed.

   """
        account = Account.create(
            self.api_client,
            self.services["account"],
            domainid=self.domain.id
   )
   self.assertIn ( self.services["account"]["accountUUID"] , account.id,
            "Account is not created with the accountId passed")

   list_account = Account.list(self.api_client,
            id=account.id
   )

   self.assertEqual(
                         isinstance(list_account, list),
                         True,
                         "Check for list account response by uuid failed"
                         )

   print (list_account)

        account_response = list_account[0]

   print (account_response)

        self.assertEqual( account_response.id,
                            account.id,
                            "listAccount response does not match with account Id "
                        )
        self.assertEqual(
                            account_response.user[0].firstname,
                            self.services["account"]["firstname"],
                            "listAccount response does not match with account firstname"
                        )

   self.cleanup.append(account)
   return

    @attr(tags=["basic", "advanced"])
    def test_createUserWithUUID(self):
   """
   Test for creating User by passing id parameter

        # Validate the following
        # 1.Create a User by passing id parameter.Verify the user is created.
        # 2.List this user by passing id parameter.Verify that list user is able to list this user.
        # 3.Delete User should succeed.

   """

        user = User.create(
            self.api_client,
            self.services["user"],
       account="Admin",
            domainid=self.domain.id
   )
   self.assertIn ( self.services["user"]["userUUID"] , user.id,
            "User is not created successfully with the userId passed")

   list_user = User.list(self.api_client,
                     id=user.id
        )

        self.assertEqual(
                         isinstance(list_user, list),
                         True,
                         "Check for list user response by uuid failed"
                         )


        user_response = list_user[0]


        self.assertEqual( user_response.id,
                            user.id,
                            "list User response does not match with user Id "
                        )
        self.assertEqual(
                            user_response.firstname,
                            self.services["user"]["firstname"],
                            "listUser response does not match with user firstname"
                        )

   user.delete(self.api_client)

        list_user = User.list(self.api_client,
                     id=user.id
        )

        self.assertIsNone(
                         list_user,
                         "Deletion of user failed"
                         )


   return

    @attr(tags=["basic", "advanced"])
    def test_createdomainWithUUID(self):
   """
   Test for creating Domain by passing id parameter

        # Validate the following
        # 1.Create a domain by passing id parameter.Verify the domain is created.
        # 2.List this domain by passing id parameter.Verify that list domain is able to list this domain.
        # 3.Delete domain should succeed.

   """

        domain = Domain.create(
            self.api_client,
            self.services["domain"]
   )
   self.assertIn ( self.services["domain"]["domainUUID"] , domain.id,
            "Domain is not created with the doaminId passed")

        list_domain = Domain.list(self.api_client,
                     id=domain.id
        )

        self.assertEqual(
                         isinstance(list_domain, list),
                         True,
                         "Check for list domain response by uuid failed"
                         )


        domain_response = list_domain[0]


        self.assertEqual( domain_response.id,
                            domain.id,
                            "list domain response does not match with domain Id "
                        )
        self.assertIn(
                            self.services["domain"]["name"],
                            domain_response.name,
                            "list domaiin response does not match with user firstname"
                        )
        try:
            domain.delete(self.api_client)
        except Exception as e:
            self.fail("Failed to delete domain: %s" % e)

   return

    @attr(tags=["basic", "advanced"])
    def test_createRegion(self):
   """
       Test for add Region
   """

   region = Region.create(self.api_client,
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

   print (region_response)
   print (self.services["region"])

        self.assertEqual(
                            str(region_response.id),
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

   region = region.delete(self.api_client)

   return

    @attr(tags=["basic", "advanced"])
    def test_createDupRegion(self):
   """
       Test for duplicate checks on id and name parameters when adding regions
   """

   self.services["region"]["regionid"]="5"
        self.services["region"]["regionname"]="Region5"
        self.services["region"]["regionendpoint"]="http://region5:8080/client"


   region = Region.create(self.api_client,
            self.services["region"]
   )

   list_region = Region.list(self.api_client,
            id=self.services["region"]["regionid"]
   )

   self.assertEqual(
                         isinstance(list_region, list),
                         True
           )
   """
        Creating regions with duplicate id should not be allowed
        """

   self.services["region"]["regionid"]="5"
        self.services["region"]["regionname"]="Region51"
        self.services["region"]["regionendpoint"]="http://region51:8080/client"

   try:
       region = Region.create(self.api_client,
            self.services["region"]
       )
       self.assertIsNone(region,
                         "Creating regions with duplicate id is allowed"
           )
   except:
       print " Creating Region with duplicate Id is not allowed"
       pass

   """
   Creating regions with duplicate name should not be allowed
   """

   self.services["region"]["regionid"]="51"
        self.services["region"]["regionname"]="Region5"
        self.services["region"]["regionendpoint"]="http://region51:8080/client"

   try:
       region = Region.create(self.api_client,
            self.services["region"]
       )
   except:
       print " Creating Region with duplicate Name is not allowed"
       pass

   region.delete(self.api_client)

   return

    @attr(tags=["basic", "advanced"])
    def test_updateRegion(self):
   """
       Test for update Region
   """

   self.services["region"]["regionid"]="3"
   self.services["region"]["regionname"]="Region3"
   self.services["region"]["regionendpoint"]="http://region3:8080/client"
   region = Region.create(self.api_client,
            self.services["region"]
   )

   self.services["region"]["regionname"]="Region3upd"
   self.services["region"]["regionendpoint"]="http://region3upd:8080/client"

   updated_region = region.update(self.api_client,
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
                            str(region_response.id),
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

   region.delete(self.api_client)

   return

    @attr(tags=["basic", "advanced"])
    def test_deleteRegion(self):
   """
       Test for delete Region
   """

   self.services["region"]["regionid"]="4"
   self.services["region"]["regionname"]="Region4"
   self.services["region"]["regionendpoint"]="http://region4:8080/client"

   region = Region.create(self.api_client,
            self.services["region"]
   )

   list_region = Region.list(self.api_client,
            id=self.services["region"]["regionid"]
   )

   print (list_region);

   self.assertEqual(
                         len(list_region),
                         1,
            "Check for Region response"
           )

   region = region.delete(self.api_client)

   list_region = Region.list(self.api_client,
            id=self.services["region"]["regionid"]
   )

   print (list_region);

   self.assertIsNone(list_region,
                         "Check for empty Region response"
                         )

   return


        @classmethod
        def tearDown(cls):

            try:
                cls.api_client = super(TestRegions, cls).getClsTestClient().getApiClient()
                #Clean up
                cleanup_resources(cls.api_client, cls.cleanup)
            except Exception as e:
                raise Exception("Warning: Exception during cleanup : %s" % e)
