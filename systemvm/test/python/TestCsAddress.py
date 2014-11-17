import unittest
from cs.CsAddress import CsAddress
import merge


class TestCsAddress(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_needs_vrrp(self):
        csaddress = CsAddress("ips", {})
        self.assertTrue(csaddress.needs_vrrp({"nw_type": "public"}))

if __name__ == '__main__':
    unittest.main()
