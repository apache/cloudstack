import unittest
from cs.CsGuestNetwork import CsGuestNetwork
import merge


class TestCsGuestNetwork(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csguestnetwork = CsGuestNetwork({}, {})
        self.assertTrue(csguestnetwork is not None)

if __name__ == '__main__':
    unittest.main()
