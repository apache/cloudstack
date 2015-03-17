import unittest
from cs.CsRoute import CsRoute
import merge


class TestCsRoute(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csroute = CsRoute(["one", "two", "three", "four"])
        self.assertTrue(csroute is not None)

if __name__ == '__main__':
    unittest.main()
