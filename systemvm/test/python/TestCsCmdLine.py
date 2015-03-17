import unittest
from cs.CsDatabag import CsCmdLine
import merge


class TestCsCmdLine(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.cscmdline = CsCmdLine('cmdline', {})

    def test_ini(self):
        self.assertTrue(self.cscmdline is not None)

    def test_idata(self):
        self.assertTrue(self.cscmdline.idata() == {})

    def test_get_priority(self):
        self.assertTrue(self.cscmdline.get_priority() == 99)

    def test_set_priority(self):
        self.cscmdline.set_priority(100)
        self.assertTrue(self.cscmdline.get_priority() == 100)

    def test_is_redundant(self):
        self.assertTrue(self.cscmdline.is_redundant() is False)
        self.cscmdline.set_redundant()
        self.assertTrue(self.cscmdline.is_redundant() is True)

    def test_get_guest_gw(self):
        self.assertTrue(self.cscmdline.get_guest_gw() == '1.2.3.4')
        tval = "192.168.1.4"
        self.cscmdline.set_guest_gw(tval)
        self.assertTrue(self.cscmdline.get_guest_gw() == tval)

if __name__ == '__main__':
    unittest.main()
