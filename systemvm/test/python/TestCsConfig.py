import unittest
from cs.CsConfig import CsConfig
import merge


class TestCsConfig(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_ini(self):
        csconfig = CsConfig(False)
        self.assertTrue(csconfig is not None)

if __name__ == '__main__':
    unittest.main()
