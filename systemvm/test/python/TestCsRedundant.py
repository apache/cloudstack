import unittest
from cs.CsRedundant import CsRedundant
from cs.CsConfig import CsConfig
import merge


class TestCsRedundant(unittest.TestCase):

    def setUp(self):
        merge.dataBag.DPATH = "."

    def test_init(self):
        csconfig = CsConfig()
        csconfig.set_cl()

        csredundant = CsRedundant(csconfig, "address")
        self.assertTrue(csredundant is not None)

if __name__ == '__main__':
    unittest.main()
