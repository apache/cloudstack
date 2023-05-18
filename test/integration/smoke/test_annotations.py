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
""" BVT tests for Hosts and Clusters
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr

#Import System modules
import time

_multiprocess_shared_ = True


class TestAnnotations(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestAnnotations, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.services['mode'] = cls.zone.networktype
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            cls.fail("get_test_template() failed to return template")

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls._cleanup = []

        # Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.userApiClient = testClient.getUserApiClient(cls.account.name, 'ROOT', 'User')

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.user_vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.user_vm)
        cls.host = list_hosts(cls.apiclient,
                               zoneid=cls.zone.id,
                               type='Routing')[0]
        cls.mgmt_server = list_mgmt_servers(cls.apiclient)[0]

    @classmethod
    def tearDownClass(cls):
        super(TestAnnotations, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = self.testClient.getParsedTestDataConfig()
        self.cleanup = []
        self.added_annotations = []

        return

    def tearDown(self):
        self.cleanAnnotations()
        super(TestAnnotations, self).tearDown()

    def cleanAnnotations(self):
        """Remove annotations"""
        for annotation in self.added_annotations:
            self.removeAnnotation(annotation.annotation.id)

    def addAnnotation(self, annotation, entityid, entitytype, adminsonly=None):
        cmd = addAnnotation.addAnnotationCmd()
        cmd.entityid = entityid
        cmd.entitytype = entitytype
        cmd.annotation = annotation
        if adminsonly:
            cmd.adminsonly = adminsonly

        self.added_annotations.append(self.apiclient.addAnnotation(cmd))

        return self.added_annotations[-1]

    def removeAnnotation(self, id):
        cmd = removeAnnotation.removeAnnotationCmd()
        cmd.id = id

        return self.apiclient.removeAnnotation(cmd)

    def getHostAnnotation(self, hostId):
        host = list_hosts(self.apiclient,
            zoneid=self.zone.id,
            type='Routing')[0]
        return host.annotation

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_add_annotation(self):
        """Testing the addAnnotations API ability to add an annoatation per host"""
        self.addAnnotation("annotation1", self.host.id, "HOST")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation1")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_add_ms_annotation(self):
        """Testing the addAnnotations API ability to add an annoatation per management server"""
        self.addAnnotation("mgmt-server-annotation1", self.mgmt_server.id, "MANAGEMENT_SERVER")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "mgmt-server-annotation1")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_add_multiple_annotations(self):
        """Testing the addAnnotations API ability to add an annoatation per host
        when there are annotations already.
        And only the last one stands as annotation attribute on host level."""
        self.addAnnotation("annotation1", self.host.id, "HOST")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation1")

        #   Adds sleep of 1 second just to be sure next annotation will not be created in the same second.
        time.sleep(1)
        self.addAnnotation("annotation2", self.host.id, "HOST")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation2")

        #   Adds sleep of 1 second just to be sure next annotation will not be created in the same second.
        time.sleep(1)
        self.addAnnotation("annotation3", self.host.id, "HOST")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation3")

        #Check that the last one is visible in host details
        self.assertEqual(self.getHostAnnotation(self.host.id), "annotation3")
        print()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_user_role_dont_infrastructure_annotations(self):
        """Testing the annotations on infrastructure are restricted to users"""

        self.addAnnotation("annotation1", self.host.id, "HOST")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation1")

        cmd = addAnnotation.addAnnotationCmd()
        cmd.entityid = self.host.id
        cmd.entitytype = "HOST"
        cmd.annotation = "test"

        try:
            self.added_annotations.append(self.userApiClient.addAnnotation(cmd))
        except Exception:
            pass
        else:
            self.fail("AddAnnotation is allowed for User")


        try:
            self.userApiClient.listAnnotations(cmd)
        except Exception:
            pass
        else:
            self.fail("ListAnnotations is allowed for User")

        cmd = removeAnnotation.removeAnnotationCmd()
        cmd.id = self.added_annotations[-1].annotation.id
        try:
            self.userApiClient.removeAnnotation(cmd)
        except Exception:
            pass
        else:
            self.fail("RemoveAnnotation is allowed for User")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_remove_annotations(self):
        """Testing the deleteAnnotation API ability to delete annotation"""
        self.addAnnotation("annotation1", self.host.id, "HOST")
        self.removeAnnotation(self.added_annotations[-1].annotation.id)
        del self.added_annotations[-1]


    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_05_add_annotation_for_invalid_entityType(self):
        cmd = addAnnotation.addAnnotationCmd()
        cmd.entityid = self.host.id
        cmd.entitytype = "BLA"
        cmd.annotation = "annotation"

        try:
            self.apiclient.addAnnotation(cmd)
        except Exception as f:
            pass
        else:
            self.fail("AddAnnotation is allowed for on an unknown entityType")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_06_add_adminsonly_and_update_annotation_visibility(self):
        """Testing admins ability to create private annotations"""

        # Admin creates an annotation only visible to admin
        self.addAnnotation("private annotation by admin", self.user_vm.id, "VM", True)
        cmd = listAnnotations.listAnnotationsCmd()
        cmd.entityid = self.user_vm.id
        cmd.entitytype = "VM"
        cmd.annotationfilter = "all"
        annotation_id = self.added_annotations[-1].annotation.id

        # Verify users cannot see private annotations created by admins
        userVisibleAnnotations = self.userApiClient.listAnnotations(cmd)
        self.assertIsNone(
            userVisibleAnnotations,
            "User must not access admin-only annotations"
        )

        # Admin updates the annotation visibility
        cmd = updateAnnotationVisibility.updateAnnotationVisibilityCmd()
        cmd.id = annotation_id
        cmd.adminsonly = False
        self.apiclient.updateAnnotationVisibility(cmd)

        # Verify user can see the annotation after updating its visibility
        cmd = listAnnotations.listAnnotationsCmd()
        cmd.entityid = self.user_vm.id
        cmd.entitytype = "VM"
        cmd.annotationfilter = "all"
        userVisibleAnnotations = self.userApiClient.listAnnotations(cmd)
        self.assertIsNotNone(
            userVisibleAnnotations,
            "User must access public annotations"
        )

        # Remove the annotation
        self.removeAnnotation(annotation_id)
        del self.added_annotations[-1]
