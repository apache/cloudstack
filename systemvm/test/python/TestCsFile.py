import unittest
from cs.CsFile import CsFile
import merge


class TestCsFile(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."

    def test_init(self):
        csfile = CsFile("testfile")
        self.assertTrue(csfile is not None)

if __name__ == '__main__':
    unittest.main()
