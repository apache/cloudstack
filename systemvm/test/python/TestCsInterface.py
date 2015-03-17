import unittest
from cs.CsAddress import CsInterface
from cs.CsConfig import CsConfig
from cs.CsDatabag import CsCmdLine
import merge


class TestCsInterface(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        csconfig = CsConfig()
        self.cmdline = CsCmdLine("cmdline", csconfig)
        csconfig.cl = self.cmdline
        self.csinterface = CsInterface({}, csconfig)

    def test_get_gateway(self):
        self.assertTrue(self.csinterface.get_gateway() == "1.2.3.4")

    def test_is_public(self):
        self.assertTrue(self.csinterface.is_public() is False)

if __name__ == '__main__':
    unittest.main()
