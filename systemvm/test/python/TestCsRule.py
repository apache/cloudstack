import unittest
from cs.CsRule import CsRule
import merge


class TestCsRule(unittest.TestCase):

    def setUp(self):
        merge.dataBag.DPATH = "."

    def test_init(self):
        csrule = CsRule(["one", "two", "three", "four"])
        self.assertTrue(csrule is not None)

if __name__ == '__main__':
    unittest.main()
