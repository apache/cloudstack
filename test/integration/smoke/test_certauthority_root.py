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

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import *
from marvin.lib.common import list_hosts

from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from OpenSSL.crypto import FILETYPE_PEM, verify, X509

PUBKEY_VERIFY=True
try:
    from OpenSSL.crypto import load_publickey
except ImportError:
    PUBKEY_VERIFY=False


class TestCARootProvider(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCARootProvider, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.cleanup = []


    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []


    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def getUpSystemVMHosts(self, hostId=None):
        hosts = list_hosts(
            self.apiclient,
            type='SecondaryStorageVM',
            state='Up',
            resourcestate='Enabled',
            id=hostId
        )
        return hosts


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_list_ca_providers(self):
        """
            Tests default ca providers list
        """
        cmd = listCAProviders.listCAProvidersCmd()
        response = self.apiclient.listCAProviders(cmd)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].name, 'root')


    def getCaCertificate(self):
        cmd = listCaCertificate.listCaCertificateCmd()
        cmd.provider = 'root'
        response = self.apiclient.listCaCertificate(cmd)
        return response.cacertificates.certificate


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_list_ca_certificate(self):
        """
            Tests the ca certificate
        """
        certificate = self.getCaCertificate()
        self.assertTrue(len(certificate) > 0)

        cert =  x509.load_pem_x509_certificate(str(certificate), default_backend())
        self.assertEqual(cert.signature_hash_algorithm.name, 'sha256')
        self.assertEqual(cert.issuer.get_attributes_for_oid(x509.oid.NameOID.COMMON_NAME)[0].value, 'ca.cloudstack.apache.org')


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_issue_certificate_without_csr(self):
        """
            Tests issuance of a certificate
        """
        cmd = issueCertificate.issueCertificateCmd()
        cmd.domain = 'apache.org,cloudstack.apache.org'
        cmd.ipaddress = '10.1.1.1,10.2.2.2'
        cmd.provider = 'root'

        response = self.apiclient.issueCertificate(cmd)
        self.assertTrue(len(response.privatekey) > 0)
        self.assertTrue(len(response.cacertificates) > 0)
        self.assertTrue(len(response.certificate) > 0)

        cert =  x509.load_pem_x509_certificate(str(response.certificate), default_backend())

        # Validate basic certificate attributes
        self.assertEqual(cert.signature_hash_algorithm.name, 'sha256')
        self.assertEqual(cert.subject.get_attributes_for_oid(x509.oid.NameOID.COMMON_NAME)[0].value, 'apache.org')

        # Validate alternative names
        altNames = cert.extensions.get_extension_for_oid(x509.oid.ExtensionOID.SUBJECT_ALTERNATIVE_NAME)
        for domain in cmd.domain.split(','):
            self.assertTrue(domain in altNames.value.get_values_for_type(x509.DNSName))
        for address in cmd.ipaddress.split(','):
            self.assertTrue(address in [str(x) for x in altNames.value.get_values_for_type(x509.IPAddress)])

        # Validate certificate against CA public key
        global PUBKEY_VERIFY
        if not PUBKEY_VERIFY:
            return
        caCert =  x509.load_pem_x509_certificate(str(self.getCaCertificate()), default_backend())
        x = X509()
        x.set_pubkey(load_publickey(FILETYPE_PEM, str(caCert.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))))
        verify(x, cert.signature, cert.tbs_certificate_bytes, cert.signature_hash_algorithm.name)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_issue_certificate_with_csr(self):
        """
            Tests issuance of a certificate
        """
        cmd = issueCertificate.issueCertificateCmd()
        cmd.csr = "-----BEGIN CERTIFICATE REQUEST-----\nMIIBHjCByQIBADBkMQswCQYDVQQGEwJJTjELMAkGA1UECAwCSFIxETAPBgNVBAcM\nCEd1cnVncmFtMQ8wDQYDVQQKDAZBcGFjaGUxEzARBgNVBAsMCkNsb3VkU3RhY2sx\nDzANBgNVBAMMBnYtMS1WTTBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQD46KFWKYrJ\nF43Y1oqWUfrl4mj4Qm05Bgsi6nuigZv7ufiAKK0nO4iJKdRa2hFMUvBi2/bU3IyY\nNvg7cdJsn4K9AgMBAAGgADANBgkqhkiG9w0BAQUFAANBAIta9glu/ZSjA/ncyXix\nyDOyAKmXXxsRIsdrEuIzakUuJS7C8IG0FjUbDyIaiwWQa5x+Lt4oMqCmpNqRzaGP\nfOo=\n-----END CERTIFICATE REQUEST-----"
        cmd.provider = 'root'

        response = self.apiclient.issueCertificate(cmd)
        self.assertTrue(response.privatekey is None)
        self.assertTrue(len(response.cacertificates) > 0)
        self.assertTrue(len(response.certificate) > 0)

        cert =  x509.load_pem_x509_certificate(str(response.certificate), default_backend())

        # Validate basic certificate attributes
        self.assertEqual(cert.signature_hash_algorithm.name, 'sha256')
        self.assertEqual(cert.subject.get_attributes_for_oid(x509.oid.NameOID.COMMON_NAME)[0].value, 'v-1-VM')

        # Validate certificate against CA public key
        global PUBKEY_VERIFY
        if not PUBKEY_VERIFY:
            return
        caCert =  x509.load_pem_x509_certificate(str(self.getCaCertificate()), default_backend())
        x = X509()
        x.set_pubkey(load_publickey(FILETYPE_PEM, str(caCert.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))))
        verify(x, cert.signature, cert.tbs_certificate_bytes, cert.signature_hash_algorithm.name)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_revoke_certificate(self):
        """
            Tests certificate revocation
        """
        cmd = revokeCertificate.revokeCertificateCmd()
        cmd.serial = 'abc123' # hex value
        cmd.cn = 'example.com'
        cmd.provider = 'root'

        self.dbclient.execute("delete from crl where serial='%s'" % cmd.serial)

        response = self.apiclient.revokeCertificate(cmd)
        self.assertTrue(response.success)

        crl = self.dbclient.execute("select serial, cn from crl where serial='%s'" % cmd.serial)[0]
        self.assertEqual(crl[0], cmd.serial)
        self.assertEqual(crl[1], cmd.cn)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_provision_certificate(self):
        """
            Tests certificate provisioning
        """
        hosts = self.getUpSystemVMHosts()
        if not hosts or len(hosts) < 1:
            raise self.skipTest("No Up systemvm hosts found, skipping test")

        host = hosts[0]

        cmd = provisionCertificate.provisionCertificateCmd()
        cmd.hostid = host.id
        cmd.reconnect = True
        cmd.provider = 'root'

        response = self.apiclient.provisionCertificate(cmd)
        self.assertTrue(response.success)

        if self.hypervisor.lower() == 'simulator':
            hosts = self.getUpSystemVMHosts(host.id)
            self.assertTrue(hosts is None or len(hosts) == 0)
        else:
            def checkHostIsUp(hostId):
                hosts = self.getUpSystemVMHosts(host.id)
                return (hosts is not None), hosts
            result, hosts = wait_until(1, 30, checkHostIsUp, host.id)
            if result:
                self.assertTrue(len(hosts) == 1)
            else:
                self.fail("Failed to have systemvm host in Up state after cert provisioning")
