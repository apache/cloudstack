import unittest
from cs.CsRedundant import CsRedundant
from cs.CsConfig import CsConfig
from cs.CsDatabag import CsCmdLine
import merge


class TestCsRedundant(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.cmdline = CsCmdLine("cmdline", {})

    def test_init(self):
        csconfig = CsConfig()
        csconfig.cl = self.cmdline
        csconfig.set_address()

        csredundant = CsRedundant(csconfig)
        self.assertTrue(csredundant is not None)

if __name__ == '__main__':
    unittest.main()
