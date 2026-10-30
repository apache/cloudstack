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

""" Integration test for the structured error-message framework.

Edits error-messages.json at runtime (no management server restart) and verifies both a
regular user and the root admin see the correct variant (including the admin-only ".admin"
suffix) on the very next API call. Handles multi-node clusters (via listManagementServers) and
local developer runs (mgtSvrIp is localhost/127.0.0.1), mirroring
test_extension_lifecycle.py's getManagementServerIps()/move_extension_path().
"""

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import listManagementServers, queryAsyncJobResult
from marvin.lib.base import Account, VirtualMachine, ServiceOffering
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.lib.utils import cleanup_resources, random_gen
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr

import inspect
import json
import logging
import os
import re
import shutil
import tempfile
import time

# resetSSHKeyForVirtualMachine is async; on failure Marvin's poller raises a generic Exception
# whose message is an unparseable ad-hoc dump of the job object (unquoted keys). We only pull
# the jobid out of it and re-query the job for properly typed fields.
JOB_ID_PATTERN = re.compile(r"jobid\s*:\s*'([0-9a-fA-F-]+)'")


class TestErrorMessageFrameworkRuntimeOverride(cloudstackTestCase):
    """Runtime error-messages.json edits must take effect without a management server
    restart, for both a regular user and the root admin."""

    ERROR_MESSAGES_PATH = "/etc/cloudstack/management/messages/error-messages.json"
    BACKUP_SUFFIX = ".bk"
    # give the file write a moment to settle before exercising the reload path
    SETTLE_DELAY_SECONDS = 3

    # resetSSHKeyForVirtualMachine's "not Stopped" check fires before any hypervisor/template
    # logic, so any Running VM reaches it deterministically, including on the simulator.
    TARGET_KEY = "vm.resetsshkey.vm.not.stopped"
    ADMIN_KEY = TARGET_KEY + ".admin"

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestErrorMessageFrameworkRuntimeOverride, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls.services["mode"] = cls.zone.networktype
        cls.template = get_template(cls.apiclient, cls.zone.id)

        cls._cleanup = []
        try:
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id,
                admin=False
            )
            cls._cleanup.append(cls.account)

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)

            cls.virtual_machine = VirtualMachine.create(
                cls.apiclient,
                cls.services["virtual_machine"],
                templateid=cls.template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                zoneid=cls.zone.id,
                mode=cls.services["mode"]
            )
            # deleting the account cascades deletion of its VM

            cls.user_apiclient = cls.testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.domain.name
            )
        except Exception as e:
            cls.tearDownClass()
            raise Exception("Failed to set up TestErrorMessageFrameworkRuntimeOverride: %s" % e)

    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.apiclient, cls._cleanup)

    def setUp(self):
        self.local_tmp_files = []

        # a leftover backup means a previous run was interrupted before it could restore
        self._recover_leftover_backup_if_any()

        original_content = self._read_error_messages_file()
        if original_content is None:
            self.skipTest(
                "Unable to read %s (via SSH or locally); skipping this test" % self.ERROR_MESSAGES_PATH
            )
        data = json.loads(original_content)

        self._backup_error_messages_file()

        # fresh markers and its own override/restore cycle per test method, so a failure in
        # one assertion can't hide whether the other would have passed
        self.base_marker = "MARVIN_BASE_%s" % random_gen()
        self.admin_marker = "MARVIN_ADMIN_%s" % random_gen()
        data[self.TARGET_KEY] = "Custom base message [%s] for {{instance}} in state {{instanceState}}" % self.base_marker
        # this key doesn't exist in the shipped file; adding it proves the whole admin-suffix
        # mechanism, not just a content edit, is picked up live
        data[self.ADMIN_KEY] = "Custom ADMIN-ONLY message [%s] for {{instance}} in state {{instanceState}}" % self.admin_marker
        self._write_error_messages_file(json.dumps(data, indent=4))

        # diagnostic: tells a write/targeting bug apart from a reload bug if this ever fails
        readback = self._read_error_messages_file()
        self.assertIsNotNone(readback, "Could not read back %s right after writing it" % self.ERROR_MESSAGES_PATH)
        self.assertIn(
            self.base_marker, readback,
            "The override was not present when reading %s back immediately after writing it" % self.ERROR_MESSAGES_PATH
        )

        time.sleep(self.SETTLE_DELAY_SECONDS)

    def tearDown(self):
        try:
            self._restore_error_messages_file_from_backup()
        finally:
            for f in self.local_tmp_files:
                try:
                    os.remove(f)
                except OSError:
                    pass

    def _get_failed_job_result(self, apiclient, exception_message):
        """Pulls the jobid out of an async failure's exception message and re-queries it for
        typed access to jobresult.errortextkey/errormetadata/errortext."""
        match = JOB_ID_PATTERN.search(exception_message)
        self.assertIsNotNone(match, "Could not find a jobid in the failure message: %s" % exception_message)
        cmd = queryAsyncJobResult.queryAsyncJobResultCmd()
        cmd.jobid = match.group(1)
        result = apiclient.queryAsyncJobResult(cmd)
        self.assertEqual(
            2, int(result.jobstatus),
            "Expected the job to have failed (jobstatus=2); got: %s" % result.jobstatus
        )
        return result.jobresult

    def _metadata_value(self, errormetadata, key):
        """errormetadata may come back as a dict or an attribute-style object."""
        if errormetadata is None:
            return None
        if isinstance(errormetadata, dict):
            return errormetadata.get(key)
        return getattr(errormetadata, key, None)

    def getManagementServerIps(self):
        """IPs of all 'Up' management servers, or None for a local (localhost/127.0.0.1) run."""
        if self.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
            return None
        cmd = listManagementServers.listManagementServersCmd()
        servers = self.apiclient.listManagementServers(cmd)
        active_server_ips = [self.mgtSvrDetails["mgtSvrIp"]]
        for server in servers:
            if server.state == 'Up' and server.ipaddress != self.mgtSvrDetails["mgtSvrIp"]:
                active_server_ips.append(server.ipaddress)
        return active_server_ips

    def _server_ssh(self, server_ip):
        return SshClient(server_ip, 22, self.mgtSvrDetails["user"], self.mgtSvrDetails["passwd"])

    def _recover_leftover_backup_if_any(self):
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            local_path = self._local_error_messages_path()
            backup_path = local_path + self.BACKUP_SUFFIX
            if os.path.isfile(backup_path):
                logging.warning("Found leftover backup %s from a previous interrupted run; restoring it", backup_path)
                shutil.move(backup_path, local_path)
            return

        backup_path = self.ERROR_MESSAGES_PATH + self.BACKUP_SUFFIX
        for server_ip in server_ips:
            sshClient = self._server_ssh(server_ip)
            try:
                result = sshClient.execute("test -f %s && echo EXISTS" % backup_path)
                if any("EXISTS" in line for line in result):
                    logging.warning("Found leftover backup %s on %s from a previous interrupted run; restoring it",
                                    backup_path, server_ip)
                    sshClient.execute("mv %s %s" % (backup_path, self.ERROR_MESSAGES_PATH))
            finally:
                sshClient.close()

    def _backup_error_messages_file(self):
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            local_path = self._local_error_messages_path()
            shutil.copy2(local_path, local_path + self.BACKUP_SUFFIX)
            return

        for server_ip in server_ips:
            sshClient = self._server_ssh(server_ip)
            try:
                sshClient.execute("cp %s %s" % (self.ERROR_MESSAGES_PATH, self.ERROR_MESSAGES_PATH + self.BACKUP_SUFFIX))
            finally:
                sshClient.close()

    def _restore_error_messages_file_from_backup(self):
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            local_path = self._local_error_messages_path()
            backup_path = local_path + self.BACKUP_SUFFIX
            if not os.path.isfile(backup_path):
                raise Exception("Backup %s is missing; cannot confirm %s was restored" % (backup_path, local_path))
            shutil.move(backup_path, local_path)
            return

        backup_path = self.ERROR_MESSAGES_PATH + self.BACKUP_SUFFIX
        for server_ip in server_ips:
            sshClient = self._server_ssh(server_ip)
            try:
                result = sshClient.execute("test -f %s && echo EXISTS" % backup_path)
                if not any("EXISTS" in line for line in result):
                    raise Exception(
                        "Backup %s is missing on %s; cannot confirm %s was restored"
                        % (backup_path, server_ip, self.ERROR_MESSAGES_PATH)
                    )
                sshClient.execute("mv %s %s" % (backup_path, self.ERROR_MESSAGES_PATH))
            finally:
                sshClient.close()

    def _find_repo_root(self):
        """Walks up from this file looking for client/pom.xml, so path resolution survives
        this file being moved elsewhere in the repo."""
        current = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
        while True:
            if os.path.isfile(os.path.join(current, "client", "pom.xml")):
                return current
            parent = os.path.dirname(current)
            if parent == current:
                return None
            current = parent

    def _local_error_messages_path(self):
        """Installed path if present; otherwise the Maven dev-build output, since a developer
        run (no package installed) serves the file straight out of the build tree."""
        if getattr(self, "_resolved_local_path", None) is not None:
            return self._resolved_local_path
        if os.path.isfile(self.ERROR_MESSAGES_PATH):
            self._resolved_local_path = self.ERROR_MESSAGES_PATH
            return self._resolved_local_path
        repo_root = self._find_repo_root()
        if repo_root is None:
            test_dir = os.path.dirname(os.path.abspath(inspect.getfile(inspect.currentframe())))
            repo_root = os.path.abspath(os.path.join(test_dir, "..", "..", ".."))
        dev_path = os.path.join(repo_root, "client", "target", "conf", "messages", "error-messages.json")
        self._resolved_local_path = dev_path
        return dev_path

    def _read_error_messages_file(self):
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            local_path = self._local_error_messages_path()
            try:
                with open(local_path) as f:
                    return f.read()
            except OSError as e:
                logging.info("Could not read %s locally: %s", local_path, e)
                return None

        # read from the primary configured server as the baseline
        sshClient = self._server_ssh(server_ips[0])
        try:
            result = sshClient.runCommand("cat %s" % self.ERROR_MESSAGES_PATH)
            if result.get("status") != "SUCCESS" or not result.get("stdout"):
                return None
            return "\n".join(result["stdout"])
        finally:
            sshClient.close()

    def _write_error_messages_file(self, content):
        server_ips = self.getManagementServerIps()
        if server_ips is None:
            local_path = self._local_error_messages_path()
            try:
                with open(local_path, "w") as f:
                    f.write(content)
            except OSError as e:
                self.fail("Failed to write %s locally: %s" % (local_path, e))
            return

        fd, local_path = tempfile.mkstemp(suffix=".json")
        self.local_tmp_files.append(local_path)
        with os.fdopen(fd, "w") as f:
            f.write(content)

        # write to every management server: whichever one serves the next API call must see it
        for idx, server_ip in enumerate(server_ips):
            logging.info("Writing %s on management server #%d with IP %s",
                         self.ERROR_MESSAGES_PATH, idx, server_ip)
            sshClient = self._server_ssh(server_ip)
            try:
                sshClient.scp(local_path, self.ERROR_MESSAGES_PATH)
            finally:
                sshClient.close()

    @attr(tags=["simulator", "basic", "advanced"])
    def test_01_runtime_error_message_override_for_regular_user(self):
        """A regular user must see the runtime-overridden base message, never the admin
        variant, with no management server restart.

        Validate the following:
        1. setUp() has already overridden vm.resetsshkey.vm.not.stopped (and added its
           .admin variant) on the live management server, with no restart.
        2. Call resetSSHKeyForVirtualMachine as a regular user on the running VM created in
           setUpClass(); it must fail since the VM is not Stopped.
        3. Re-query the failed job and confirm errortextkey is the base key.
        4. Confirm errortext contains the base marker, not the admin marker.
        5. Confirm errormetadata.instanceState reflects the VM's real state.
        """

        self.debug("Calling resetSSHKeyForVirtualMachine as a regular user on a running VM")
        with self.assertRaises(Exception) as ctx:
            self.virtual_machine.resetSshKey(
                self.user_apiclient,
                keypair=random_gen() + ".pem"
            )
        job_result = self._get_failed_job_result(self.user_apiclient, str(ctx.exception))

        self.assertEqual(
            self.TARGET_KEY, job_result.errortextkey,
            "errortextkey should be the base key regardless of caller role; got: %s" % job_result.errortextkey
        )
        self.assertTrue(
            self.base_marker in job_result.errortext,
            "Regular user should see the runtime-overridden base message; got: %s" % job_result.errortext
        )
        self.assertFalse(
            self.admin_marker in job_result.errortext,
            "Regular user must NOT see the admin-only variant; got: %s" % job_result.errortext
        )
        self.assertEqual(
            "Running", self._metadata_value(job_result.errormetadata, "instanceState"),
            "errormetadata.instanceState should reflect the VM's real state"
        )

    @attr(tags=["simulator", "basic", "advanced"])
    def test_02_runtime_error_message_override_for_admin(self):
        """The root admin must see the runtime-added '.admin' variant, with no management
        server restart.

        Validate the following:
        1. setUp() has already overridden vm.resetsshkey.vm.not.stopped and added its .admin
           variant on the live management server, with no restart.
        2. Call resetSSHKeyForVirtualMachine as the root admin on the same running VM; it must
           fail since the VM is not Stopped.
        3. Re-query the failed job and confirm errortextkey is still the base key (only the
           resolved errortext should differ by caller role).
        4. Confirm errortext contains the admin marker.
        5. Confirm errormetadata.instanceState reflects the VM's real state.
        """

        self.debug("Calling resetSSHKeyForVirtualMachine as the root admin on a running VM")
        with self.assertRaises(Exception) as ctx:
            self.virtual_machine.resetSshKey(
                self.apiclient,
                keypair=random_gen() + ".pem"
            )
        job_result = self._get_failed_job_result(self.apiclient, str(ctx.exception))

        self.assertEqual(
            self.TARGET_KEY, job_result.errortextkey,
            "errortextkey should be the base key even when the .admin template is used; got: %s" % job_result.errortextkey
        )
        self.assertTrue(
            self.admin_marker in job_result.errortext,
            "Root admin should see the runtime-added .admin variant; got: %s" % job_result.errortext
        )
        self.assertEqual(
            "Running", self._metadata_value(job_result.errormetadata, "instanceState"),
            "errormetadata.instanceState should reflect the VM's real state"
        )
