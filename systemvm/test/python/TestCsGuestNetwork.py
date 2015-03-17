import unittest
from cs.CsGuestNetwork import CsGuestNetwork
import merge


class TestCsGuestNetwork(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csguestnetwork = CsGuestNetwork({}, {})
        self.assertTrue(csguestnetwork is not None)

    def test_get_dns(self):
        csguestnetwork = CsGuestNetwork({}, {})
        csguestnetwork.guest = True
        csguestnetwork.set_dns("1.1.1.1,2.2.2.2")
        csguestnetwork.set_router("3.3.3.3")
        dns = csguestnetwork.get_dns()
        self.assertTrue(len(dns) == 3)
        csguestnetwork.set_dns("1.1.1.1")
        dns = csguestnetwork.get_dns()
        self.assertTrue(len(dns) == 2)

if __name__ == '__main__':
    unittest.main()
