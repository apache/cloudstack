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
""" BVT tests for custom extension
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import listManagementServers
from marvin.lib.base import (Extension,
                             Pod,
                             Cluster,
                             Host,
                             Template,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             ExtensionCustomAction)
from marvin.lib.common import (get_zone)
from marvin.lib.utils import (random_gen)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr
# Import System modules
import json
import logging
from pathlib import Path
import random
import string
import time

_multiprocess_shared_ = True

CUSTOM_EXTENSION_CONTENT = """#!/bin/bash

parse_json() {
  local json_string=$1
  echo "$json_string" | jq '.' > /dev/null || { echo '{"error":"Invalid JSON input"}'; exit 1; }
}

get_vm_name() {
  local input_json=$1
  local name
  name=$(jq -r '.virtualmachinename' <<< "$input_json")
  if [[ -z "$name" || "$name" == "null" ]]; then
      echo '{"status":"error","message":"virtualmachinename missing in JSON"}'
      exit 1
  fi
  echo "$name"
}

get_vm_file() {
  local name=$1
  local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  echo "$script_dir/$name"
}

validate_vm_file_exists() {
  local file=$1
  if [[ ! -f "$file" ]]; then
      echo '{"status":"error","message":"Instance file not found"}'
      exit 1
  fi
}

update_vm_status() {
  local file=$1
  local status=$2
  local updated_json
  updated_json=$(jq --arg status "$status" '.status = $status' "$file")
  echo "$updated_json" > "$file"
}

prepare() {
  echo ""
}

create() {
  parse_json "$1"
  local vm_name updated_json file
  vm_name=$(get_vm_name "$1")
  file=$(get_vm_file "$vm_name")

  updated_json=$(jq '. + {status: "Running"}' <<< "$1")
  echo "$updated_json" > "$file"

  jq -n --arg file "$file" \
      '{status: "success", message: "Instance created", file: $file}'
}

delete() {
  parse_json "$1"
  local vm_name file
  vm_name=$(get_vm_name "$1")
  file=$(get_vm_file "$vm_name")

  if [[ -f "$file" ]]; then
      rm -f "$file"
  fi
  jq -n --arg file "$file" \
      '{status: "success", message: "Instance deleted", file: $file}'
}

start() {
  parse_json "$1"
  local vm_name file
  vm_name=$(get_vm_name "$1")
  file=$(get_vm_file "$vm_name")
  validate_vm_file_exists "$file"

  update_vm_status "$file" "Running"
  echo '{"status": "success", "message": "Instance started"}'
}

stop() {
  parse_json "$1"
  local vm_name file
  vm_name=$(get_vm_name "$1")
  file=$(get_vm_file "$vm_name")

  if [[ -f "$file" ]]; then
      update_vm_status "$file" "Stopped"
  fi
  echo '{"status": "success", "message": "Instance stopped"}'
}

reboot() {
  parse_json "$1"
  local vm_name file
  vm_name=$(get_vm_name "$1")
  file=$(get_vm_file "$vm_name")
  validate_vm_file_exists "$file"

  update_vm_status "$file" "Running"
  echo '{"status": "success", "message": "Instance rebooted"}'
}

status() {
  parse_json "$1"
  local vm_name file vm_status
  vm_name=$(get_vm_name "$1")
  file=$(get_vm_file "$vm_name")
  validate_vm_file_exists "$file"

  vm_status=$(jq -r '.status' "$file")
  [[ -z "$vm_status" || "$vm_status" == "null" ]] && vm_status="unknown"

  case "${vm_status,,}" in
    "running"|"poweron")
      power_state="PowerOn"
      ;;
    "stopped"|"shutdown"|"poweroff")
      power_state="PowerOff"
      ;;
    *)
      power_state="$vm_status"
      ;;
  esac

  jq -n --arg ps "$power_state" '{status: "success", power_state: $ps}'
}

testaction() {
  parse_json "$1"
  local vm_name param_val
  vm_name=$(get_vm_name "$1")
  param_val=$(jq -r '.parameters.Name' <<< "$1")

  echo "$param_val for $vm_name"
}

action=$1
parameters_file="$2"
wait_time="$3"

if [[ -z "$action" || -z "$parameters_file" ]]; then
  echo '{"error":"Missing required arguments"}'
  exit 1
fi

if [[ ! -r "$parameters_file" ]]; then
  echo '{"error":"File not found or unreadable"}'
  exit 1
fi

parameters=$(<"$parameters_file")

case $action in
  prepare)     prepare "$parameters" ;;
  create)      create "$parameters" ;;
  delete)      delete "$parameters" ;;
  start)       start "$parameters" ;;
  stop)        stop "$parameters" ;;
  reboot)      reboot "$parameters" ;;
  status)      status "$parameters" ;;
  testaction)  testaction "$parameters" ;;
  *)           echo '{"error":"Invalid action"}'; exit 1 ;;
esac

exit 0

"""

class TestExtensions(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestExtensions, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())

        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        cls._cleanup = []
        cls.logger = logging.getLogger('TestExtensions')
        cls.logger.setLevel(logging.DEBUG)

        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"])
        cls._cleanup.append(cls.compute_offering)
        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["l2-network_offering"],
        )
        cls._cleanup.append(cls.network_offering)
        cls.network_offering.update(cls.apiclient, state='Enabled')
        cls.services["network"]["networkoffering"] = cls.network_offering.id
        cls.l2_network = Network.create(
            cls.apiclient,
            cls.services["l2-network"],
            zoneid=cls.zone.id,
            networkofferingid=cls.network_offering.id
        )
        cls._cleanup.append(cls.l2_network)

        cls.resource_name_suffix = random_gen()
        cls.create_cluster()
        cls.extension = Extension.create(
            cls.apiclient,
            name=f"ext-{cls.resource_name_suffix}",
            type='Orchestrator'
        )
        cls._cleanup.append(cls.extension)
        cls.update_extension_path(cls.extension.path)
        cls.extension.register(cls.apiclient, cls.cluster.id, 'Cluster')
        details = {
            'url': f"host-{cls.resource_name_suffix}",
            'zoneid': cls.cluster.zoneid,
            'podid': cls.cluster.podid,
            'username': 'External',
            'password': 'External'
        }
        cls.host = Host.create(
            cls.apiclient,
            cls.cluster,
            details,
            hypervisor=cls.cluster.hypervisortype
        )
        cls._cleanup.append(cls.host)
        template_name = f"template-{cls.resource_name_suffix}"
        template_data = {
            "name": template_name,
            "displaytext": template_name,
            "format": cls.host.hypervisor,
            "hypervisor": cls.host.hypervisor,
            "ostype": "Other Linux (64-bit)",
            "url": template_name,
            "requireshvm": "True",
            "ispublic": "True",
            "isextractable": "True",
            "extensionid": cls.extension.id
        }
        cls.template = Template.register(
            cls.apiclient,
            template_data,
            zoneid=cls.zone.id,
            hypervisor=cls.cluster.hypervisortype
        )
        cls._cleanup.append(cls.template)
        logging.info("Waiting for 3 seconds for template to be ready")
        time.sleep(3)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

    @classmethod
    def tearDownClass(cls):
        super(TestExtensions, cls).tearDownClass()

    @classmethod
    def create_cluster(cls):
        pod_list = Pod.list(cls.apiclient)
        if len(pod_list) <= 0:
            cls.fail("No Pods found")
        pod_id = pod_list[0].id
        cluster_services = {
            'clustername': 'cluster-' + cls.resource_name_suffix,
            'clustertype': 'CloudManaged'
        }
        cls.cluster = Cluster.create(
            cls.apiclient,
            cluster_services,
            cls.zone.id,
            pod_id,
            'External'
        )
        cls._cleanup.append(cls.cluster)

    @classmethod
    def getManagementServerIps(cls):
        if cls.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
            return None
        cmd = listManagementServers.listManagementServersCmd()
        servers = cls.apiclient.listManagementServers(cmd)
        active_server_ips = []
        active_server_ips.append(cls.mgtSvrDetails["mgtSvrIp"])
        for idx, server in enumerate(servers):
            if server.state == 'Up' and server.ipaddress != cls.mgtSvrDetails["mgtSvrIp"]:
                active_server_ips.append(server.ipaddress)
        return active_server_ips

    @classmethod
    def update_path_locally(cls, path):
        try:
            file = Path(path)
            file.write_text(CUSTOM_EXTENSION_CONTENT)
            file.chmod(file.stat().st_mode | 0o111)  # Make executable
        except Exception as e:
            cls.fail(f"Failed to update path on localhost: {str(e)}")

    @classmethod
    def update_extension_path(cls, path):
        logging.info(f"Updating extension path {path}")
        server_ips = cls.getManagementServerIps()
        if server_ips is None:
            if cls.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
                cls.update_path_locally(path)
                return
            cls.fail(f"Extension path update cannot be done on {cls.mgtSvrDetails['mgtSvrIp']}")
        logging.info("Updating extension path on all management server")
        command = (
            f"cat << 'EOF' > {path}\n{CUSTOM_EXTENSION_CONTENT}\nEOF\n"
            f"chmod +x {path}"
        )
        for idx, server_ip in enumerate(server_ips):
            logging.info(f"Updating extension path on management server #{idx} with IP {server_ip}")
            sshClient = SshClient(
                server_ip,
                22,
                cls.mgtSvrDetails["user"],
                cls.mgtSvrDetails["passwd"]
            )
            sshClient.execute(command)

    def setUp(self):
        self.cleanup = []

    def tearDown(self):
        super(TestExtensions, self).tearDown()

    def get_vm_content_path(self, extension_path, vm_name):
        directory = extension_path.rsplit('/', 1)[0] if '/' in extension_path else '.'
        path = f"{directory}/{vm_name}"
        return path

    def get_vm_content_locally(self, path):
        try:
            file = Path(path)
            if not file.exists():
                return None
            return file.read_text().strip()
        except Exception:
            return None

    def get_vm_content(self, extension_path, vm_name):
        path = self.get_vm_content_path(extension_path, vm_name)
        server_ips = self.getManagementServerIps()
        if not server_ips:
            if self.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
                return self.get_vm_content_locally(path)
            return None
        logging.info("Trying to get VM content from all management server")
        command = f"cat {path}"
        for idx, server_ip in enumerate(server_ips):
            logging.info(f"Trying to get VM content from management server #{idx} with IP {server_ip}")
            sshClient = SshClient(
                server_ip,
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"]
            )
            results = sshClient.execute(command)
            if isinstance(results, list) and len(results) > 0:
                return '\n'.join(line.strip() for line in results)
        return None

    def check_vm_content_values(self, content):
        if content is None:
            self.fail("VM content is empty")

        try:
            data = json.loads(content)
        except json.JSONDecodeError as e:
            self.fail(f"Invalid JSON for the VM: {e}")

        if not data:
            self.fail("Empty JSON for the VM")

        required_keys = ['externaldetails', 'cloudstack.vm.details', 'virtualmachineid', 'virtualmachinename', 'status']
        if not all(key in data for key in required_keys):
            self.fail("Missing one or more required keys.")

        memory = self.compute_offering.memory * 1024 * 1024
        vm_details = data['cloudstack.vm.details']

        self.assertEqual(
            memory,
            vm_details['minRam'],
            "VM memory mismatch"
        )
        self.assertEqual(
            self.compute_offering.cpunumber,
            vm_details['cpus'],
            "VM CPU count mismatch"
        )
        self.assertEqual(
            'Running',
            data['status'],
            "VM status mismatch"
        )

    def check_vm_status_from_content(self, extension_path, vm_name, status):
        content = self.get_vm_content(extension_path, vm_name)
        if content is None:
            self.fail("VM content is empty")

        try:
            data = json.loads(content)
        except json.JSONDecodeError as e:
            self.fail(f"Invalid JSON for the VM: {e}")

        if not data:
            self.fail("Empty JSON for the VM")

        self.assertEqual(
            status,
            data['status'],
            "VM status mismatch"
        )

    def check_vm_content_exist_locally(self, path):
        try:
            file = Path(path)
            return file.exists()
        except Exception:
            return None

    def check_vm_content_exist(self, extension_path, vm_name):
        path = self.get_vm_content_path(extension_path, vm_name)
        server_ips = self.getManagementServerIps()
        if not server_ips:
            if self.mgtSvrDetails["mgtSvrIp"] in ('localhost', '127.0.0.1'):
                return self.check_vm_content_exist_locally(path)
            return None
        logging.info("Trying to check VM content exists from all management server")
        command = f"test -e '{path}' && echo EXISTS || echo MISSING"
        for idx, server_ip in enumerate(server_ips):
            logging.info(f"Trying to check VM content exists from management server #{idx} with IP {server_ip}")
            sshClient = SshClient(
                server_ip,
                22,
                self.mgtSvrDetails["user"],
                self.mgtSvrDetails["passwd"]
            )
            results = sshClient.execute(command)
            if results is not None and results[0].strip() == "EXISTS":
                return True
        return False

    def popItemFromCleanup(self, item_id):
        for idx, x in enumerate(self.cleanup):
            if x.id == item_id:
                self.cleanup.pop(idx)
                break

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_01_extension_vm_lifecycle(self):
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.compute_offering.id,
            networkids=[self.l2_network.id]
        )
        self.cleanup.append(self.virtual_machine)
        self.assertEqual(
            self.virtual_machine.state,
            'Running',
            "VM not in Running state"
        )
        content = self.get_vm_content(self.extension.path, self.virtual_machine.instancename)
        self.check_vm_content_values(content)
        self.virtual_machine.stop(self.apiclient)
        self.check_vm_status_from_content(self.extension.path, self.virtual_machine.instancename, 'Stopped')
        self.virtual_machine.start(self.apiclient)
        self.check_vm_status_from_content(self.extension.path, self.virtual_machine.instancename, 'Running')
        self.virtual_machine.reboot(self.apiclient)
        self.check_vm_status_from_content(self.extension.path, self.virtual_machine.instancename, 'Running')
        self.virtual_machine.delete(self.apiclient, expunge=True)
        self.popItemFromCleanup(self.virtual_machine.id)
        self.assertFalse(
            self.check_vm_content_exist(self.extension.path, self.virtual_machine.instancename),
            "VM content exist event after expunge"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_02_run_custom_action(self):
        name = 'testaction'
        details = [{}]
        details[0]['abc'] = 'xyz'
        parameters = [{}]
        parameter0 = {
            'name': 'Name',
            'type': 'STRING',
            'validationformat': 'NONE',
            'required': True
        }
        parameters[0] = parameter0
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=name,
            enabled=True,
            details=details,
            parameters=parameters,
            successmessage='Successfully completed {{actionName}}'
        )
        self.cleanup.append(self.custom_action)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.compute_offering.id,
            networkids=[self.l2_network.id]
        )
        self.cleanup.append(self.virtual_machine)
        param_val=random_gen()
        run_parameters = [{}]
        run_parameters[0] = {
            'Name': param_val
        }
        run_response = self.custom_action.run(
            self.apiclient,
            resourceid=self.virtual_machine.id,
            parameters=run_parameters
        )
        self.assertTrue(
            run_response.success,
            "Action run status not success"
        )
        self.assertEquals(
            f"Successfully completed {name}",
            run_response.result.message,
            "Action run status not success"
        )
        data = run_response.result.details
        self.assertEquals(
            f"{param_val} for {self.virtual_machine.instancename}",
            run_response.result.details,
            "Action response details not match"
        )

    @attr(tags=["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"], required_hardware="false")
    def test_03_run_invalid_custom_action(self):
        name = 'invalidaction'
        self.custom_action = ExtensionCustomAction.create(
            self.apiclient,
            extensionid=self.extension.id,
            name=name,
            enabled=True,
            errormessage='Failed {{actionName}}'
        )
        self.cleanup.append(self.custom_action)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.compute_offering.id,
            networkids=[self.l2_network.id]
        )
        self.cleanup.append(self.virtual_machine)
        run_response = self.custom_action.run(
            self.apiclient,
            resourceid=self.virtual_machine.id
        )
        self.assertFalse(
            run_response.success,
            "Action run status not failure"
        )
        self.assertEquals(
            f"Failed {name}",
            run_response.result.message,
            "Action run status not success"
        )
