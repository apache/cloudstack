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

"""Helpers for systemvm tests that run the VR scripts against a temporary databag directory.

VrSandbox copies a cmdline databag and an ips databag from fixtures/router
into a temporary folder and points the databag path at it, so the tests never
read or write /etc/cloudstack. It also resets the cached cmdline on CsConfig,
which other tests share. close() restores both.

FakeShell records the commands the scripts pass to CsHelper.execute and
CsHelper.execute2.
"""

import json
import os
import shutil
import tempfile

from cs.CsConfig import CsConfig
import merge

# Folders searched in order by fixture_path().
FIXTURE_DIRS = [os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures", "router")]


def fixture_path(name):
    for directory in FIXTURE_DIRS:
        path = os.path.join(directory, name)
        if os.path.exists(path):
            return path
    return os.path.join(FIXTURE_DIRS[0], name)


def load_json(name):
    with open(fixture_path(name)) as f:
        return json.load(f)


def load_conf(name):
    """Read an expected frr.conf fixture. Lines starting with # are the licence header and notes."""
    with open(fixture_path(name)) as f:
        return [line.rstrip('\n') for line in f if not line.startswith("#")]


class VrSandbox(object):
    """A VR databag directory in a temporary folder.

    mode is "natted" or "routed" and picks the cmdline fixture.
    ips is a fixture name or a dict. extra maps databag file names to their contents.
    """

    def __init__(self, mode="natted", ips="ips.json", extra=None):
        self.dir = tempfile.mkdtemp(prefix="vr-test-")
        self.old_dpath = merge.DataBag.DPATH
        self.old_cl = CsConfig.cl
        self.old_gn = CsConfig.gn
        shutil.copy(fixture_path("cmdline-%s.json" % mode), os.path.join(self.dir, "cmdline.json"))
        if isinstance(ips, dict):
            self.write("ips.json", ips)
        else:
            shutil.copy(fixture_path(ips), os.path.join(self.dir, "ips.json"))
        for name, data in (extra or {}).items():
            self.write(name, data)
        merge.DataBag.DPATH = self.dir
        CsConfig.cl = None
        CsConfig.gn = None
        self.config = CsConfig()
        self.config.cmdline()
        self.config.set_address()

    def write(self, name, data):
        with open(os.path.join(self.dir, name), "w") as f:
            json.dump(data, f)

    def path(self, name):
        return os.path.join(self.dir, name)

    def close(self):
        merge.DataBag.DPATH = self.old_dpath
        CsConfig.cl = self.old_cl
        CsConfig.gn = self.old_gn
        shutil.rmtree(self.dir, ignore_errors=True)


class FakeShell(object):
    """Records CsHelper.execute and CsHelper.execute2 commands. execute returns no output.

    execute2 returns a process result with the given return code. waited lists the
    execute2 commands run with wait=True, and communicated those whose output was read.
    """

    def __init__(self, returncode=0):
        self.commands = []
        self.returncode = returncode
        self.waited = []
        self.communicated = []

    def execute(self, command):
        self.commands.append(command)
        return []

    def execute2(self, command, wait=True):
        self.commands.append(command)
        if wait:
            self.waited.append(command)
        shell = self
        shell_returncode = self.returncode

        class Result(object):
            returncode = shell_returncode

            def communicate(self):
                shell.communicated.append(command)
                return (b"", b"")

            def wait(self):
                return self.returncode
        return Result()
