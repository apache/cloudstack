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
""" Test for Direct Downloads of Templates and ISOs
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (ServiceOffering,
                             NetworkOffering,
                             Network,
                             Template,
                             VirtualMachine)
from marvin.lib.common import (get_pod,
                               get_zone)
from nose.plugins.attrib import attr
from marvin.cloudstackAPI import (uploadTemplateDirectDownloadCertificate, revokeTemplateDirectDownloadCertificate)
from marvin.lib.decoratorGenerators import skipTestIf


class TestUploadDirectDownloadCertificates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUploadDirectDownloadCertificates, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls._cleanup = []
        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() not in ['kvm', 'lxc']:
            cls.hypervisorNotSupported = True

        if not cls.hypervisorNotSupported:
            cls.certificates = {
                "expired": "MIIDSTCCAjECFDi8s70TWFhwVN9cj67RJoAF99c8MA0GCSqGSIb3DQEBCwUAMGExCzAJBgNVBAYTAkNTMQswCQYDVQQIDAJDUzELMAkGA1UEBwwCQ1MxCzAJBgNVBAoMAkNTMQswCQYDVQQLDAJDUzELMAkGA1UEAwwCQ1MxETAPBgkqhkiG9w0BCQEWAkNTMB4XDTE5MDQyNDE1NTQxM1oXDTE5MDQyMjE1NTQxM1owYTELMAkGA1UEBhMCQ1MxCzAJBgNVBAgMAkNTMQswCQYDVQQHDAJDUzELMAkGA1UECgwCQ1MxCzAJBgNVBAsMAkNTMQswCQYDVQQDDAJDUzERMA8GCSqGSIb3DQEJARYCQ1MwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCrLS0XDBgqOvtoaI0TIxLropp1qdu8n2IQ1MPwo9NzCXvygjocLA6l/NkDSK/2zbS3RX9mSRFEHdPLCy5R3lMvcuWjnMbfUhsdo8aJajuDAS+wVt3ZJTtCL42hHwXhT+rnc/Go3lbq/4jz2W9hHXdM5V7h7w6M30IrL26biSJp01FcdEXglz9O+TwRr5akF1trhIbfhP8Nx/P9q62tyRVeiecXO+yskEqtrdftmVOzyYsv66aV3+a407zZusnX2oPP2r+5XILNp7XZCuJKAJpJkToQMDaJ5S+3A8SAvN3CjqjhYkF0sK9SIKC3+8wTWnyqE5Um/3r+mbdfAVxDCxqZAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAG/R9sJ2pFbu35MliIJIhWkwP7FeP/7gYCNvOXFt6vVGXmcOwuw9WGBxsmsGESQRB4+NnJFjyGQ1Ck+ps5XRRMizyvq6bCQxVuC5M+vYS4J0q8YoL0RJ20pN9iwTsosZjSEKmfUlVgsufqCG2nyusV71LSaQU6f/bylJcJkKwGUhThExh+PVLZ66H5cF4/SzuK6WzWnj5p6+YX8TP+qPUkXN1mapgVKfVMo6mqLsH+eLKH+zqdy5ZZ5znNSbJFgHufYbEFlutTaxHEvKNMEgMCFkFGiyPwRuD6oaPnZFquJLh/mBZOLogpxVD5v20AcUTANtbXSlPaqOnEQFcbiVCb8=",
                "invalid": "XXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                "valid": "MIIDSzCCAjMCFDa0LoW+1O8/cEwCI0nIqfl8c1TLMA0GCSqGSIb3DQEBCwUAMGExCzAJBgNVBAYTAkNTMQswCQYDVQQIDAJDUzELMAkGA1UEBwwCQ1MxCzAJBgNVBAoMAkNTMQswCQYDVQQLDAJDUzELMAkGA1UEAwwCQ1MxETAPBgkqhkiG9w0BCQEWAkNTMCAXDTE5MDQyNDE1NTIzNVoYDzIwOTgwOTE1MTU1MjM1WjBhMQswCQYDVQQGEwJDUzELMAkGA1UECAwCQ1MxCzAJBgNVBAcMAkNTMQswCQYDVQQKDAJDUzELMAkGA1UECwwCQ1MxCzAJBgNVBAMMAkNTMREwDwYJKoZIhvcNAQkBFgJDUzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKstLRcMGCo6+2hojRMjEuuimnWp27yfYhDUw/Cj03MJe/KCOhwsDqX82QNIr/bNtLdFf2ZJEUQd08sLLlHeUy9y5aOcxt9SGx2jxolqO4MBL7BW3dklO0IvjaEfBeFP6udz8ajeVur/iPPZb2Edd0zlXuHvDozfQisvbpuJImnTUVx0ReCXP075PBGvlqQXW2uEht+E/w3H8/2rra3JFV6J5xc77KyQSq2t1+2ZU7PJiy/rppXf5rjTvNm6ydfag8/av7lcgs2ntdkK4koAmkmROhAwNonlL7cDxIC83cKOqOFiQXSwr1IgoLf7zBNafKoTlSb/ev6Zt18BXEMLGpkCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAVS5uWZRz2m3yx7EUQm47RTMW5WMXU4pI8D+N5WZ9xubYOqtU3r2OAYpfL/QO8iT7jcqNYGoDqe8ZjEaNvfxiTG8cOI6TSXhKBG6hjSaSFQSHOZ5mfstM36y/3ENFh6JCJ2ao1rgWSbfDRyAaHuvt6aCkaV6zRq2OMEgoJqZSgwxLQO230xa2hYgKXOePMVZyHFA2oKJtSOc3jCke9Y8zDUwm0McGdMRBD8tVB0rcaOqQ0PlDLjB9sQuhhLu8vjdgbznmPbUmMG7JN0yhT1eJbIX5ImXyh0DoTwiaGcYwW6SqYodjXACsC37xaQXAPYBiaAs4iI80TJSx1DVFO1LV0g=="
            }

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"], required_hardware="false")
    def test_01_sanity_check_on_certificates(self):
        """Test Verify certificates before uploading to KVM hosts
        """

        # Validate the following
        # 1. Invalid certificates cannot be uploaded to hosts for direct downloads
        # 2. Expired certificates cannot be uploaded to hosts for direct downloads

        cmd = uploadTemplateDirectDownloadCertificate.uploadTemplateDirectDownloadCertificateCmd()
        cmd.hypervisor = self.hypervisor
        cmd.name = "marvin-test-verify-certs"
        cmd.certificate = self.certificates["invalid"]
        cmd.zoneid = self.zone.id

        invalid_cert_uploadFails = False
        expired_cert_upload_fails = False
        try:
            self.apiclient.uploadTemplateDirectDownloadCertificate(cmd)
            self.fail("Invalid certificate must not be uploaded")
        except Exception as e:
            invalid_cert_uploadFails = True

        cmd.certificate = self.certificates["expired"]
        try:
            self.apiclient.uploadTemplateDirectDownloadCertificate(cmd)
            self.fail("Expired certificate must not be uploaded")
        except Exception as e:
            expired_cert_upload_fails = True

        self.assertTrue(invalid_cert_uploadFails and expired_cert_upload_fails,
                        "Invalid or expired certificates must not be uploaded")
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"], required_hardware="false")
    def test_02_upload_direct_download_certificates(self):
        """Test Upload certificates to KVM hosts for direct download
        """

        # Validate the following
        # 1. Valid certificates are uploaded to hosts
        # 2. Revoke uploaded certificate from host

        cmd = uploadTemplateDirectDownloadCertificate.uploadTemplateDirectDownloadCertificateCmd()
        cmd.hypervisor = self.hypervisor
        cmd.name = "marvin-test-verify-certs"
        cmd.certificate = self.certificates["valid"]
        cmd.zoneid = self.zone.id

        try:
            self.apiclient.uploadTemplateDirectDownloadCertificate(cmd)
        except Exception as e:
            self.fail("Valid certificate must be uploaded")

        revokecmd = revokeTemplateDirectDownloadCertificate.revokeTemplateDirectDownloadCertificateCmd()
        revokecmd.hypervisor = self.hypervisor
        revokecmd.name = cmd.name
        revokecmd.zoneid = self.zone.id

        try:
            self.apiclient.revokeTemplateDirectDownloadCertificate(revokecmd)
        except Exception as e:
            self.fail("Uploaded certificates should be revoked when needed")

        return


class TestDirectDownloadTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDirectDownloadTemplates, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.dbclient = cls.testClient.getDbConnection()
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls._cleanup = []
        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() not in ['kvm', 'lxc']:
            cls.hypervisorNotSupported = True

        if not cls.hypervisorNotSupported:
            cls.services["test_templates"]["kvm"]["directdownload"] = "true"
            cls.template = Template.register(cls.apiclient, cls.services["test_templates"]["kvm"],
                              zoneid=cls.zone.id, hypervisor=cls.hypervisor)
            cls._cleanup.append(cls.template)

            cls.services["virtual_machine"]["zoneid"] = cls.zone.id
            cls.services["virtual_machine"]["template"] = cls.template.id
            cls.services["virtual_machine"]["hypervisor"] = cls.hypervisor
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offerings"]["tiny"]
            )
            cls._cleanup.append(cls.service_offering)
            cls.network_offering = NetworkOffering.create(
                cls.apiclient,
                cls.services["l2-network_offering"],
            )
            cls.network_offering.update(cls.apiclient, state='Enabled')
            cls.services["network"]["networkoffering"] = cls.network_offering.id
            cls.l2_network = Network.create(
                cls.apiclient,
                cls.services["l2-network"],
                zoneid=cls.zone.id,
                networkofferingid=cls.network_offering.id
            )
            cls._cleanup.append(cls.l2_network)
            cls._cleanup.append(cls.network_offering)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "basic", "eip", "advancedns", "sg"], required_hardware="false")
    def test_01_deploy_vm_from_direct_download_template(self):
        """Test Deploy VM from direct download template
        """

        # Validate the following
        # 1. Register direct download template
        # 2. Deploy VM from direct download template

        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            serviceofferingid=self.service_offering.id,
            networkids=self.l2_network.id
        )
        self.assertEqual(
            vm.state,
            "Running",
            "Check VM deployed from direct download template is running"
        )
        self.cleanup.append(vm)
        return
