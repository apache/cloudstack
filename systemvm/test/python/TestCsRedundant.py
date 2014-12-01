import unittest
from cs.CsRedundant import CsRedundant
from cs.CsConfig import CsConfig
import merge


class TestCsRedundant(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csconfig = CsConfig()
        csconfig.set_cl()
        csconfig.set_address()

        csredundant = CsRedundant(csconfig)
        self.assertTrue(csredundant is not None)

if __name__ == '__main__':
    unittest.main()
