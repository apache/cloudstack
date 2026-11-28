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

import os
import shutil
import tempfile
import unittest
import mock
from cs import CsLoadBalancer as lbmod
import merge


class TestCsLoadBalancer(unittest.TestCase):

    def setUp(self):
        merge.DataBag.DPATH = "."
        self.tmpdir = tempfile.mkdtemp()
        self.conf_t = os.path.join(self.tmpdir, "haproxy.cfg.new")
        self.conf_p = os.path.join(self.tmpdir, "haproxy.cfg")
        with open(self.conf_p, "w") as f:
            f.write("global\n    old\n")
        patchers = [
            mock.patch.object(lbmod, "HAPROXY_CONF_T", self.conf_t),
            mock.patch.object(lbmod, "HAPROXY_CONF_P", self.conf_p),
            mock.patch.object(lbmod.CsLoadBalancer, "_verify_haproxy_config", return_value=""),
            mock.patch.object(lbmod, "CsProcess"),
        ]
        for p in patchers:
            p.start()
            self.addCleanup(p.stop)
        self.addCleanup(shutil.rmtree, self.tmpdir)

        # Skip CsDataBag.__init__, which loads the databag from disk
        self.lb = lbmod.CsLoadBalancer.__new__(lbmod.CsLoadBalancer)
        self.lb.config = mock.Mock()
        self.lb.config.get_fw.return_value = []
        self.lb.dbag = {"config": [{
            "configuration": ["global\n    new"],
            "add_rules": ["10.1.1.1:80"],
            "remove_rules": [],
            "stat_rules": [],
        }]}

    @mock.patch.object(lbmod.CsHelper, "service")
    def test_process_installs_new_config_and_reloads(self, mock_service):
        self.lb.process()
        with open(self.conf_p) as f:
            self.assertEqual("global\n    new\n", f.read())
        self.assertEqual(1, mock_service.call_count)
        self.assertEqual(1, len(self.lb.config.get_fw.return_value))

    @mock.patch.object(lbmod.CsHelper, "service")
    @mock.patch("shutil.copy2", side_effect=PermissionError(1, "Operation not permitted"))
    def test_process_fails_when_config_cannot_be_installed(self, mock_copy, mock_service):
        with self.assertRaises(Exception) as ctx:
            self.lb.process()
        self.assertIn("Could not install new haproxy config", str(ctx.exception))
        mock_copy.assert_called_once_with(self.conf_t, self.conf_p)
        mock_service.assert_not_called()
        self.assertEqual([], self.lb.config.get_fw.return_value)
        with open(self.conf_p) as f:
            self.assertEqual("global\n    old\n", f.read())


if __name__ == '__main__':
    unittest.main()
