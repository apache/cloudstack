from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (
    TrafficType,
    PhysicalNetwork,
    Zone,
)
from nose.plugins.attrib import attr


class TestTrafficTypeApi(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.test_client = super(TestTrafficTypeApi, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls._cleanup = []
        cls.services["advanced_sg"]["zone"]["name"] = "TestTrafficTypeApi-zone"
        cls.zone = Zone.create(cls.api_client, cls.services["advanced_sg"]["zone"])
        cls._cleanup.append(cls.zone)

        cls.physical_network = PhysicalNetwork.create(
            cls.api_client,
            cls.services["l2-network"],
            isolationmethods="VLAN",
            zoneid=cls.zone.id,
        )
        cls._cleanup.append(cls.physical_network)

    @attr(tags=["advanced"], required_hardware="false")
    def test_list_api_fields(self):
        traffic_type = TrafficType.add(
            self.api_client,
            physicalnetworkid=self.physical_network.id,
            kvmnetworklabel="kvm",
            traffictype="Public",
            vlan="100",
        ).traffictype

        traffic_types = TrafficType.list(
            self.api_client,
            physicalnetworkid=self.physical_network.id
        )

        assert len(traffic_types) == 1
        response = traffic_types[0]
        self.assertEqual(response.id, traffic_type.id)
        self.assertEqual(response.kvmnetworklabel, "kvm")
        self.assertEqual(response.traffictype, "Public")
        self.assertEqual(response.vlan, "100")
        self.assertEqual(response.isolationmethods, "VLAN")
