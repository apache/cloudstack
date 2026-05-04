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

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import *
from nose.plugins.attrib import attr

import subprocess
import time
import logging
import socket

class TestCloudStackDNSFramework(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        """
        Pre-requisite:
        Bring up PDNS via docker compose (external dependency for DNS provider).
        """
        super(TestCloudStackDNSFramework, cls).setUpClass()
        cls.api_client = cls.testClient.getApiClient()

        cls.logger = logging.getLogger("TestCloudStackDNSFramework")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)
        # -------------------------
        # Detect Marvin VM IP (reachable by MS)
        # -------------------------
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(("8.8.8.8", 80))
            cls.marvin_vm_ip = s.getsockname()[0]
        finally:
            s.close()

        cls.logger.info(f"Detected Marvin VM IP: {cls.marvin_vm_ip}")

        # -------------------------
        # PDNS compose config
        # -------------------------

        cls.compose_file = "/marvin/pdns/docker-compose.yml"
        cls.compose_dir = "/marvin/pdns"
        cls.logger.info("Bringing up PDNS via docker compose...")

        up_cmd = [
            "docker", "compose",
            "-f", cls.compose_file,
            "up", "-d"
        ]

        result = subprocess.run(
            up_cmd,
            cwd=cls.compose_dir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        if result.returncode != 0:
            cls.tearDownClass()
            raise Exception(f"Failed to start PDNS:\n{result.stderr}")

        # Allow PDNS to initialize
        time.sleep(15)

        cls.logger.info("PDNS is up and running")

        # Construct PDNS URL once
        cls.pdns_url = f"http://{cls.marvin_vm_ip}"
        cls.logger.info(f"PDNS endpoint: {cls.pdns_url}")

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_list_dns_providers(self):
        """
        List DNS providers, expect PowerDNS provider to be present
        """
        list_providers_cmd = listDnsProviders.listDnsProvidersCmd()
        self.logger.info("Listing DNS providers to verify PowerDNS presence")
        response = self.api_client.listDnsProviders(list_providers_cmd)
        self.assertIsNotNone(response, "Failed to list DNS providers")
        self.logger.info(f"DNS Providers found: {[provider.name for provider in response]}")


    @attr(tags=["advanced"], required_hardware="true")
    def test_02_add_dns_server(self):
        """
        Register PDNS as DNS provider in CloudStack
        """
        self.logger.info("Adding PDNS DNS server")

        response = self._add_dns_server()
        self.assertIsNotNone(response, "Failed to add DNS provider")
        self.__class__.dns_server_id = response.id
        self.logger.info(f"DNS Provider added: {response.id}")
        self.assertIsNotNone(response.id, "DNS server ID should not be None")


    @attr(tags=["advanced"], required_hardware="true")
    def test_03_list_dns_servers(self):
        """
        List DNS servers and verify the newly added PDNS provider is present
        """
        self.logger.info("Listing DNS servers to verify addition")
        list_cmd = listDnsServers.listDnsServersCmd()
        list_cmd.id = self.dns_server_id
        response = self.api_client.listDnsServers(list_cmd)
        self.assertIsNotNone(response, "Failed to list DNS servers")
        self.assertEqual(len(response), 1, "Expected exactly one DNS server")
        self.assertEqual(response[0].id, self.dns_server_id, "DNS server ID mismatch")


    @attr(tags=["advanced"], required_hardware="true")
    def test_04_create_dns_zone(self):
        """
        Create a DNS zone in the added PDNS provider
        """
        self.logger.info("Creating a DNS zone")
        response = self._create_zone(self.dns_server_id)
        self.assertIsNotNone(response, "Failed to create DNS zone")
        self.assertIsNotNone(response.id, "DNS zone ID should not be None")
        self.__class__.dns_zone_id = response.id
        self.logger.info(f"DNS Zone created: {response.id}")


    @attr(tags=["advanced"], required_hardware="true")
    def test_05_list_dns_zones(self):
        """
        List DNS zones and verify the newly created zone is present
        """
        self.logger.info("Listing DNS zones to verify creation")
        list_zones_cmd = listDnsZones.listDnsZonesCmd()
        list_zones_cmd.id = self.dns_zone_id
        response = self.api_client.listDnsZones(list_zones_cmd)
        self.assertIsNotNone(response, "Failed to list DNS zones")
        self.assertEqual(len(response), 1, "Expected exactly one DNS zone")
        self.assertEqual(response[0].id, self.dns_zone_id, "DNS zone ID mismatch")
        self.assertEqual(response[0].name, "example.com", "DNS zone name mismatch")

    @attr(tags=["advanced"], required_hardware="true")
    def test_06_create_a_dns_record(self):
        """
        Create a DNS record in the previously created zone
        """
        self.logger.info("Creating A DNS record")
        response = self._create_record(
            self.dns_zone_id,
            "www.example.com",
            "A",
            "10.1.1.10"
        )
        self.assertIsNotNone(response, "Failed to create DNS record")
        self.assertEqual(response.name, "www.example.com", "DNS record name mismatch")
        self._assert_dns("www.example.com", "A", expected="10.1.1.10")

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_create_aaaa_dns_records(self):
        """
        Create AAAA DNS records in the previously created zone
        """
        self.logger.info("Creating AAAA DNS records")
        response = self._create_record(
            self.dns_zone_id,
            "www.example.com",
            "AAAA",
            "2001:db8::10"
        )
        self.assertIsNotNone(response, "Failed to create AAAA DNS record")
        self.assertTrue(response.name is not None, "DNS record name should not be None")
        self._assert_dns("www.example.com", "AAAA", expected="2001:db8::10")

    @attr(tags=["advanced"], required_hardware="true")
    def test_08_create_mx_dns_record(self):
        """
        Create an MX DNS record in the previously created zone
        """
        self.logger.info("Creating an MX DNS record")
        response = self._create_record(
            self.dns_zone_id,
            "example.com",
            "MX",
            "10 mail.example.com"
        )
        self.assertIsNotNone(response, "Failed to create MX DNS record")
        self.assertTrue(response.name is not None, "DNS record name should not be None")
        self._assert_dns("example.com", "MX", contains=["10", "mail.example.com"])


    @attr(tags=["advanced"], required_hardware="true")
    def test_09_list_dns_records(self):
        """
        List DNS records in the zone and verify the created records are present
        """
        self.logger.info("Listing DNS records to verify creation")
        list_records_cmd = listDnsRecords.listDnsRecordsCmd()
        list_records_cmd.dnszoneid = self.dns_zone_id
        response = self.api_client.listDnsRecords(list_records_cmd)
        self.assertIsNotNone(response, "Failed to list DNS records")
        self.assertEqual(len(response), 4, "Expected four DNS records, including NS record")
        record_types = set(record.type for record in response)
        self.assertSetEqual(record_types, {"NS", "A", "AAAA", "MX"}, "DNS record types mismatch")

    @attr(tags=["advanced"], required_hardware="true")
    def test_10_delete_dns_record(self):
        """
        Delete one of the DNS records and verify it's removed
        """
        self.logger.info("Deleting a DNS record")
        delete_record_cmd = deleteDnsRecord.deleteDnsRecordCmd()
        delete_record_cmd.name = "www.example.com"
        delete_record_cmd.type = "A"
        delete_record_cmd.dnszoneid = self.dns_zone_id
        delete_response = self.api_client.deleteDnsRecord(delete_record_cmd)
        self.assertIsNotNone(delete_response, "Failed to delete DNS record")
        self.logger.info(f"DNS Record deleted: {delete_record_cmd.name}")

        # Verify deletion
        list_record_cmd = listDnsRecords.listDnsRecordsCmd()
        list_record_cmd.dnszoneid = self.dns_zone_id
        response_after_deletion = self.api_client.listDnsRecords(list_record_cmd)
        self.assertEqual(len(response_after_deletion), 3, "Expected three DNS records after deletion")
        remaining_record_names = set(record.name for record in response_after_deletion)
        self.assertNotIn(delete_record_cmd.name, remaining_record_names, "Deleted DNS record still present")

    @attr(tags=["advanced"], required_hardware="true")
    def test_11_delete_dns_zone(self):
        """
        Delete the DNS zone and verify it's removed
        """
        self.logger.info("Deleting the DNS zone")
        delete_zone_cmd = deleteDnsZone.deleteDnsZoneCmd()
        delete_zone_cmd.id = self.dns_zone_id
        response = self.api_client.deleteDnsZone(delete_zone_cmd)
        self.assertIsNotNone(response, "Failed to delete DNS zone")
        self.logger.info(f"DNS Zone deleted: {self.dns_zone_id}")

        # Verify deletion
        list_zones_cmd = listDnsZones.listDnsZonesCmd()
        list_zones_cmd.id = self.dns_zone_id
        try:
            self.api_client.listDnsZones(list_zones_cmd)
            self.fail("DNS zone still exists after deletion")
        except Exception as e:
            self.logger.info(f"Expected exception after delete: {str(e)}")

    @attr(tags=["advanced"], required_hardware="true")
    def test_12_delete_dns_server(self):
        """
        Delete the PDNS DNS server and verify it's removed
        """
        self.logger.info("Deleting the PDNS DNS server")
        delete_cmd = deleteDnsServer.deleteDnsServerCmd()
        delete_cmd.id = self.dns_server_id
        response = self.api_client.deleteDnsServer(delete_cmd)
        self.assertIsNotNone(response, "Failed to delete DNS server")
        self.logger.info(f"DNS Server deleted: {self.dns_server_id}")

        # Verify deletion
        list_cmd = listDnsServers.listDnsServersCmd()
        list_cmd.id = self.dns_server_id
        response = self.api_client.listDnsServers(list_cmd)
        dns_servers = response or []
        self.assertEqual(len(dns_servers), 0, "Expected no DNS servers after deletion")

    @classmethod
    def tearDownClass(cls):
        """
        Stop PDNS after tests
        """

        try:
            cls.logger.info("Stopping PDNS stack...")

            cmd = [
                "docker", "compose",
                "-f", cls.compose_file,
                "down"
            ]

            subprocess.run(cmd, cwd=cls.compose_dir)

        finally:
            super(TestCloudStackDNSFramework, cls).tearDownClass()


    def _create_record(self, zone_id, name, rtype, contents):
        cmd = createDnsRecord.createDnsRecordCmd()
        cmd.dnszoneid = zone_id
        cmd.name = name
        cmd.type = rtype
        cmd.contents = contents

        return self.api_client.createDnsRecord(cmd)


    def _add_dns_server(self):
        cmd = addDnsServer.addDnsServerCmd()
        cmd.name = "pdns-server"
        cmd.url = self.pdns_url
        cmd.dnsapikey = "supersecretapikey"
        cmd.provider = "PowerDNS"
        cmd.nameservers = ["ns1.example.com", "ns2.example.com"]
        cmd.externalserverid = "localhost"
        cmd.ispublic = True
        cmd.port = 8081
        cmd.publicdomainsuffix = "pdns-public.example.com"

        return self.api_client.addDnsServer(cmd)


    def _create_zone(self, server_id):
        cmd = createDnsZone.createDnsZoneCmd()
        cmd.dnsserverid = server_id
        cmd.name = "example.com"
        cmd.description = "Test DNS Zone for PDNS"

        return self.api_client.createDnsZone(cmd)


    def _dig(self, name, rtype):
        dns_ip = self.__class__.marvin_vm_ip
        dns_port = 53

        cmd = [
            "dig",
            f"@{dns_ip}",
            "-p",
            str(dns_port),
            name,
            rtype,
            "+short"
        ]
        self.logger.info(f"Running: {' '.join(cmd)}")
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        self.assertEqual(result.returncode, 0, f"dig failed: {result.stderr}")
        output = result.stdout.strip().replace("\n", " ")
        self.logger.info(f"dig output: {output}")
        return output


    def _assert_dns(self, name, rtype, expected=None, contains=None):
        output = self._dig(name, rtype)

        if expected is not None:
            self.assertIn(expected, output)

        if contains:
            for item in contains:
                self.assertIn(item, output)

        return output
