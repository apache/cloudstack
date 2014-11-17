import unittest
import mock
from cs.CsDhcp import CsDhcp
from cs import CsHelper
import merge


class TestCsDhcp(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    @mock.patch('cs.CsDhcp.CsHelper')
    @mock.patch('cs.CsDhcp.CsDnsMasq')
    def test_init(self, mock_cshelper, mock_dnsmasq):
        csdhcp = CsDhcp({}, None)
        self.assertTrue(csdhcp is not None)

if __name__ == '__main__':
    unittest.main()
