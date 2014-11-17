import unittest
from cs.CsNetfilter import CsNetfilter
import merge


class TestCsNetfilter(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csnetfilter = CsNetfilter()
        self.assertTrue(csnetfilter is not None)

if __name__ == '__main__':
    unittest.main()
