import unittest
from cs.CsAddress import CsAddress
import merge


class TestCsAddress(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.csaddress = CsAddress("ips", {})

    def test_needs_vrrp(self):
        self.assertTrue(self.csaddress.needs_vrrp({"nw_type": "guest"}))

    def test_get_guest_if(self):
        self.assertTrue(self.csaddress.get_guest_if() is None)

    def test_get_guest_ip(self):
        self.assertTrue(self.csaddress.get_guest_ip() is None)

    def test_get_guest_netmask(self):
        self.assertTrue(self.csaddress.get_guest_netmask() == "255.255.255.0")

if __name__ == '__main__':
    unittest.main()
