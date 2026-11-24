# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""The router version health check reports the router's BGP tooling.

The output of router_version_check.py gains one line with three tokens:

    frrreload=yes|no bfdd=yes|no frrmajor=<n>|none

The management server parses that line to tell whether frr-reload.py and
bfdd are present and which FRR major version runs. The script provides:

- FRR_RELOAD_PATH and BFDD_PATH: module constants for the two files.
- frr_version(): the installed FRR version as a string such as "8.4.4",
  or None when it cannot tell.
- capability_report(): returns the line above.

main() prints the line on every run, also when the routerVersion data is
missing, and the report never changes the exit code. A failing check can
recreate the VR, and a missing tool is no reason for that.
"""

import io
import os
import re
import shutil
import sys
import tempfile
import unittest
try:
    import mock
except ImportError:
    from unittest import mock

HEALTH_CHECKS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "debian", "root", "health_checks")
if HEALTH_CHECKS not in sys.path:
    sys.path.insert(0, HEALTH_CHECKS)

try:
    import router_version_check
    IMPORT_ERROR = None
except ImportError as e:
    router_version_check = None
    IMPORT_ERROR = e

REPORT = re.compile(r'(?m)^frrreload=(yes|no) bfdd=(yes|no) frrmajor=(\d+|none)$')


class TestRouterVersionCheckCapabilities(unittest.TestCase):

    def setUp(self):
        if router_version_check is None:
            self.fail("router_version_check.py cannot be imported: %s" % IMPORT_ERROR)
        self.dir = tempfile.mkdtemp(prefix="vrcheck-test-")
        self.addCleanup(shutil.rmtree, self.dir, True)
        self.reload_path = os.path.join(self.dir, "frr-reload.py")
        self.bfdd_path = os.path.join(self.dir, "bfdd")

    def require(self, *names):
        for name in names:
            if not hasattr(router_version_check, name):
                self.fail("router_version_check.%s is missing" % name)

    def tools(self, reload=True, bfdd=True, version="8.4.4"):
        """Point the script at temporary tool files and a fixed FRR version."""
        self.require("FRR_RELOAD_PATH", "BFDD_PATH", "frr_version", "capability_report")
        for present, p in ((reload, self.reload_path), (bfdd, self.bfdd_path)):
            if present:
                open(p, "w").close()
        for patch in (mock.patch.object(router_version_check, "FRR_RELOAD_PATH", self.reload_path),
                      mock.patch.object(router_version_check, "BFDD_PATH", self.bfdd_path),
                      mock.patch.object(router_version_check, "frr_version", return_value=version)):
            patch.start()
            self.addCleanup(patch.stop)

    def run_main(self, data):
        """Run main() with the given routerVersion entries. Returns (stdout, exit code)."""
        out = io.StringIO()
        code = 0
        with mock.patch.object(router_version_check, "getHealthChecksData", return_value=data), \
                mock.patch("sys.stdout", out):
            try:
                router_version_check.main()
            except SystemExit as e:
                code = e.code or 0
        return out.getvalue(), code

    def report(self, text):
        m = REPORT.search(text)
        self.assertIsNotNone(m, "no capability line in: %r" % text)
        return m.groups()

    def test_report_all_present(self):
        self.tools()
        self.assertEqual(self.report(router_version_check.capability_report()), ("yes", "yes", "8"))

    def test_report_frr_reload_missing_only(self):
        # An old template with refreshed scripts: bfdd is there, frr-pythontools is not.
        self.tools(reload=False)
        self.assertEqual(self.report(router_version_check.capability_report())[:2], ("no", "yes"))

    def test_main_prints_report_beside_version_result(self):
        # The existing version result stays. The report is one more line.
        self.tools()
        with mock.patch.object(router_version_check, "getFirstLine", return_value="4.22.0"):
            out, code = self.run_main([{"templateVersion": "4.22.0"}])
        self.assertIn("Template and scripts version match successful", out)
        self.assertEqual(self.report(out), ("yes", "yes", "8"))
        self.assertEqual(code, 0)

    def test_missing_tools_do_not_fail_the_check(self):
        # The report is information. A missing tool must not mark the check failed.
        self.tools(reload=False, bfdd=False, version=None)
        out, code = self.run_main(None)
        self.assertEqual(self.report(out), ("no", "no", "none"))
        self.assertEqual(code, 0)


if __name__ == '__main__':
    unittest.main()
