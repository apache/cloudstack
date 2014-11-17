import unittest
from cs.CsApp import CsApp
from cs.CsAddress import CsIP
from cs.CsConfig import CsConfig
import merge


class TestCsApp(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csconfig = CsConfig()
        csconfig.set_cl()
        csip = CsIP("eth0", csconfig);
        csapp = CsApp(csip)
        self.assertTrue(csapp is not None)

if __name__ == '__main__':
    unittest.main()
