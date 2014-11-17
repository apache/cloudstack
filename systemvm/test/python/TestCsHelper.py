import unittest
import mock
from cs import CsHelper
import merge


class TestCsHelper(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    @mock.patch('cs.CsHelper.subprocess')
    def test_execute(self, mock_subprocess):
        result = CsHelper.execute("/bin/false")
        self.assertTrue(result is not None)

if __name__ == '__main__':
    unittest.main()
