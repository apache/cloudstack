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

import re
from datetime import datetime, timedelta

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, wait_until
from marvin.lib.base import *
from marvin.lib.common import list_hosts

from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding


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


    def verifySignature(self, caCert, cert):
        print("Verifying Certificate")
        caPublicKey = caCert.public_key()
        try:
            caPublicKey.verify(
                cert.signature,
                cert.tbs_certificate_bytes,
                padding.PKCS1v15(),
                cert.signature_hash_algorithm,
            )
            print("Certificate is valid!")
        except Exception as e:
            print(f"Certificate verification failed: {e}")


    def parseCertificateChain(self, pem):
        """Split a PEM blob containing one or more certificates into a list of x509 objects."""
        certs = []
        matches = re.findall(
            r'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----',
            pem,
            re.DOTALL
        )
        for match in matches:
            certs.append(x509.load_pem_x509_certificate(match.encode(), default_backend()))
        return certs


    def assertSignatureValid(self, issuerCert, cert):
        """Verify cert is signed by issuerCert; raise on failure."""
        issuerCert.public_key().verify(
            cert.signature,
            cert.tbs_certificate_bytes,
            padding.PKCS1v15(),
            cert.signature_hash_algorithm,
        )

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

        cert =  x509.load_pem_x509_certificate(certificate.encode(), default_backend())
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

        cert =  x509.load_pem_x509_certificate(response.certificate.encode(), default_backend())

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
        caCert =  x509.load_pem_x509_certificate(self.getCaCertificate().encode(), default_backend())
        self.verifySignature(caCert, cert)


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
        cert =  x509.load_pem_x509_certificate(response.certificate.encode(), default_backend())

        # Validate basic certificate attributes
        self.assertEqual(cert.signature_hash_algorithm.name, 'sha256')
        self.assertEqual(cert.subject.get_attributes_for_oid(x509.oid.NameOID.COMMON_NAME)[0].value, 'v-1-VM')

        # Validate certificate against CA public key
        caCert =  x509.load_pem_x509_certificate(self.getCaCertificate().encode(), default_backend())
        self.verifySignature(caCert, cert)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_revoke_certificate(self):
        """
            Tests certificate revocation
        """
        cmd = revokeCertificate.revokeCertificateCmd()
        cmd.serial = 'abc123' # hex value
        cmd.cn = 'example.com'
        cmd.provider = 'root'
        serials = self.dbclient.execute(f"select serial, cn from crl where serial='{cmd.serial}'")
        if len(serials) > 0:
            self.dbclient.execute(f"delete from crl where serial='{cmd.serial}'")

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


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_ca_certificate_chain_validity(self):
        """
            Tests that listCaCertificate returns a valid certificate chain.
            When an intermediate CA is configured, the response is a PEM blob
            containing multiple certificates. Each non-root cert must be signed
            by the next cert in the chain, and the final cert must be self-signed.
        """
        pem = self.getCaCertificate()
        self.assertTrue(len(pem) > 0)

        chain = self.parseCertificateChain(pem)
        self.assertTrue(len(chain) >= 1, "Expected at least one certificate in CA chain")

        # Each non-root cert must be signed by the next cert in the chain
        for i in range(len(chain) - 1):
            child = chain[i]
            parent = chain[i + 1]
            self.assertEqual(
                child.issuer, parent.subject,
                f"Chain break: cert[{i}] issuer does not match cert[{i + 1}] subject"
            )
            try:
                self.assertSignatureValid(parent, child)
            except Exception as e:
                self.fail(f"Signature verification failed for chain link {i} -> {i + 1}: {e}")

        # The last cert in the chain must be self-signed (root CA)
        root = chain[-1]
        self.assertEqual(
            root.issuer, root.subject,
            "Final cert in CA chain is not self-signed"
        )
        try:
            self.assertSignatureValid(root, root)
        except Exception as e:
            self.fail(f"Root CA self-signature verification failed: {e}")


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_issue_certificate_issuer_matches_ca(self):
        """
            Tests that an issued certificate's issuer DN matches the subject DN
            of the first cert in the returned CA chain, and that the signature
            verifies against that cert's public key.
        """
        cmd = issueCertificate.issueCertificateCmd()
        cmd.domain = 'apache.org'
        cmd.ipaddress = '10.1.1.1'
        cmd.provider = 'root'

        response = self.apiclient.issueCertificate(cmd)
        self.assertTrue(len(response.certificate) > 0)
        self.assertTrue(len(response.cacertificates) > 0)

        leaf = x509.load_pem_x509_certificate(response.certificate.encode(), default_backend())
        caChain = self.parseCertificateChain(response.cacertificates)
        self.assertTrue(len(caChain) >= 1, "Expected at least one CA certificate in response")

        # The issuing CA is the first cert in the returned chain (intermediate
        # if an intermediate CA is configured, otherwise the root).
        issuingCa = caChain[0]
        self.assertEqual(
            leaf.issuer, issuingCa.subject,
            "Leaf certificate issuer does not match issuing CA subject"
        )
        try:
            self.assertSignatureValid(issuingCa, leaf)
        except Exception as e:
            self.fail(f"Leaf certificate signature does not verify against issuing CA: {e}")


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_certificate_validity_period(self):
        """
            Tests that an issued certificate has sensible validity bounds:
            not_valid_before <= now <= not_valid_after, and validity duration
            is at least 300 days (CloudStack default is 1 year).
        """
        cmd = issueCertificate.issueCertificateCmd()
        cmd.domain = 'apache.org'
        cmd.provider = 'root'

        response = self.apiclient.issueCertificate(cmd)
        self.assertTrue(len(response.certificate) > 0)

        cert = x509.load_pem_x509_certificate(response.certificate.encode(), default_backend())

        # cryptography >= 42 prefers the *_utc variants; fall back for older versions.
        notBefore = getattr(cert, 'not_valid_before_utc', None) or cert.not_valid_before
        notAfter = getattr(cert, 'not_valid_after_utc', None) or cert.not_valid_after

        now = datetime.now(notBefore.tzinfo) if notBefore.tzinfo else datetime.utcnow()
        self.assertTrue(notBefore <= now, f"Certificate not_valid_before {notBefore} is in the future")
        self.assertTrue(now <= notAfter, f"Certificate not_valid_after {notAfter} is in the past")

        duration = notAfter - notBefore
        self.assertTrue(
            duration >= timedelta(days=300),
            f"Certificate validity duration {duration} is less than expected minimum of 300 days"
        )


    def getUpKVMHosts(self, hostId=None):
        hosts = list_hosts(
            self.apiclient,
            type='Routing',
            hypervisor='KVM',
            state='Up',
            resourcestate='Enabled',
            id=hostId
        )
        return hosts


    @attr(tags=['advanced'], required_hardware=True)
    def test_provision_certificate_kvm(self):
        """
            Tests certificate provisioning on a KVM host.
            Exercises the keystore-cert-import + cloud.jks provisioning flow
            against a real agent. Skipped when no KVM hosts are available.
        """
        if self.hypervisor.lower() != 'kvm':
            raise self.skipTest("Hypervisor is not KVM, skipping test")

        hosts = self.getUpKVMHosts()
        if not hosts or len(hosts) < 1:
            raise self.skipTest("No Up KVM hosts found, skipping test")

        host = hosts[0]

        cmd = provisionCertificate.provisionCertificateCmd()
        cmd.hostid = host.id
        cmd.reconnect = True
        cmd.provider = 'root'

        response = self.apiclient.provisionCertificate(cmd)
        self.assertTrue(response.success)

        def checkHostIsUp(hostId):
            hosts = self.getUpKVMHosts(hostId)
            return (hosts is not None and len(hosts) > 0), hosts

        result, hosts = wait_until(2, 30, checkHostIsUp, host.id)
        if not result:
            self.fail("KVM host did not return to Up state after certificate provisioning")
        self.assertEqual(len(hosts), 1)
