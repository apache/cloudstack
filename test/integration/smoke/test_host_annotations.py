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

class TestHostAnnotations(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.host = list_hosts(self.apiclient,
            zoneid=self.zone.id,
            type='Routing')[0]
        self.cleanup = []
        self.added_annotations = []

        return

    def tearDown(self):
        try:
            #Clean up
            cleanup_resources(self.apiclient, self.cleanup)
            self.cleanAnnotations()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def cleanAnnotations(self):
        """Remove annotations"""
        for annotation in self.added_annotations:
            self.removeAnnotation(annotation.annotation.id)

    def addAnnotation(self, annotation):
        cmd = addAnnotation.addAnnotationCmd()
        cmd.entityid = self.host.id
        cmd.entitytype = "HOST"
        cmd.annotation = annotation

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
        self.addAnnotation("annotation1")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation1")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_add_multiple_annotations(self):
        """Testing the addAnnotations API ability to add an annoatation per host
        when there are annotations already.
        And only the last one stands as annotation attribute on host level."""
        self.addAnnotation("annotation1")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation1")

        #   Adds sleep of 1 second just to be sure next annotation will not be created in the same second.
        time.sleep(1)
        self.addAnnotation("annotation2")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation2")

        #   Adds sleep of 1 second just to be sure next annotation will not be created in the same second.
        time.sleep(1)
        self.addAnnotation("annotation3")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation3")

        #Check that the last one is visible in host details
        self.assertEqual(self.getHostAnnotation(self.host.id), "annotation3")
        print()

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_user_role_dont_see_annotations(self):
        """Testing the annotations api are restricted to users"""

        self.addAnnotation("annotation1")
        self.assertEqual(self.added_annotations[-1].annotation.annotation, "annotation1")

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
        )
        self.cleanup.append(self.account)

        userApiClient = self.testClient.getUserApiClient(self.account.name, 'ROOT', 'User')

        cmd = addAnnotation.addAnnotationCmd()
        cmd.entityid = self.host.id
        cmd.entitytype = "HOST"
        cmd.annotation = "test"

        try:
            self.added_annotations.append(userApiClient.addAnnotation(cmd))
        except Exception:
            pass
        else:
            self.fail("AddAnnotation is allowed for User")

        cmd = listAnnotations.listAnnotationsCmd()
        try:
            userApiClient.listAnnotations(cmd)
        except Exception:
            pass
        else:
            self.fail("ListAnnotations is allowed for User")

        cmd = removeAnnotation.removeAnnotationCmd()
        cmd.id = self.added_annotations[-1].annotation.id
        try:
            userApiClient.removeAnnotation(cmd)
        except Exception:
            pass
        else:
            self.fail("RemoveAnnotation is allowed for User")

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_04_remove_annotations(self):
        """Testing the deleteAnnotation API ability to delete annotation"""
        self.addAnnotation("annotation1")
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

