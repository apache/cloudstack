#!/usr/bin/env python3
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

import warnings
warnings.filterwarnings('ignore')

import json
import sys
import winrm


def fail(message):
    print(json.dumps({"status": "error", "error": message}))
    sys.exit(1)


def succeed(data):
    print(json.dumps(data))
    sys.exit(0)


class HyperVManager:
    def __init__(self, config_path):
        self.config_path = config_path
        self.data = self.parse_json()
        self.session = self.init_winrm_session()

    def parse_json(self):
        try:
            with open(self.config_path, 'r') as f:
                json_data = json.load(f)

            external_host_details = json_data["externaldetails"].get("host", [])
            data = {
                "url": external_host_details["url"],
                "username": external_host_details["username"],
                "password": external_host_details["password"],
                "network_switch": external_host_details["network_switch"],
                "vhd_path": external_host_details["vhd_path"],
                "vm_path": external_host_details["vm_path"],
                "cert_validation": "validate" if external_host_details.get("verify_tls_certificate", "true").lower() == "true" else "ignore"
            }

            external_vm_details = json_data["externaldetails"].get("virtualmachine", [])
            if external_vm_details:
                data["template_type"] = external_vm_details["template_type"]
                data["generation"] = external_vm_details.get("generation", 1)
                data["template_path"] = external_vm_details.get("template_path", "")
                data["iso_path"] = external_vm_details.get("iso_path", "")
                data["vhd_size_gb"] = external_vm_details.get("vhd_size_gb", "")

            data["cpus"] = json_data["cloudstack.vm.details"]["cpus"]
            data["memory"] = json_data["cloudstack.vm.details"]["minRam"]
            data["vmname"] = json_data["cloudstack.vm.details"]["name"]

            nics = json_data["cloudstack.vm.details"].get("nics", [])
            data["nics"] = []
            for nic in nics:
                data["nics"].append({
                    "mac": nic["mac"],
                    "vlan": nic["broadcastUri"].replace("vlan://", "")
                })

            parameters = json_data.get("parameters", [])
            if parameters:
                data["snapshot_name"] = parameters.get("snapshot_name", "")

            return data

        except KeyError as e:
            fail(f"Missing required field in JSON: {str(e)}")
        except Exception as e:
            fail(f"Error parsing JSON: {str(e)}")

    def init_winrm_session(self):
        return winrm.Session(
            f"https://{self.data['url']}:5986/wsman",
            auth=(self.data["username"], self.data["password"]),
            transport='ntlm',
            server_cert_validation=self.data["cert_validation"]
        )

    def run_ps_int(self, command):
        r = self.session.run_ps(command)
        if r.status_code != 0:
            raise Exception(r.std_err.decode())
        return r.std_out.decode()

    def run_ps(self, command):
        try:
            output = self.run_ps_int(command)
            return output
        except Exception as e:
            fail(str(e))

    def vm_not_present(self, exception):
        vm_not_present_str = f'Hyper-V was unable to find a virtual machine with name "{self.data["vmname"]}"'
        return vm_not_present_str in str(exception)

    def create(self):
        vm_name = self.data["vmname"]
        cpus = self.data["cpus"]
        memory = self.data["memory"]
        memory_mb = int(memory) / 1024 / 1024
        template_path = self.data["template_path"]
        vhd_path = self.data["vhd_path"] + "\\" + vm_name + ".vhdx"
        vhd_size_gb = self.data["vhd_size_gb"]
        generation = self.data["generation"]
        iso_path = self.data["iso_path"]
        network_switch = self.data["network_switch"]
        vm_path = self.data["vm_path"]
        template_type = self.data.get("template_type", "template")

        vhd_created = False
        vm_created = False
        vm_started = False
        try:
            command = (
                f'New-VM -Name "{vm_name}" -MemoryStartupBytes {memory_mb}MB '
                f'-Generation {generation}  -Path "{vm_path}" '
            )
            if template_type == "iso":
                if (iso_path == ""):
                    fail("Missing required field in JSON: iso_path")
                if (vhd_size_gb == ""):
                    fail("Missing required field in JSON: vhd_size_gb")
                command += (
                    f'-NewVHDPath "{vhd_path}" -NewVHDSizeBytes {vhd_size_gb}GB; '
                    f'Add-VMDvdDrive -VMName "{vm_name}" -Path "{iso_path}"; '
                )
            else:
                if (template_path == ""):
                    fail("Missing required field in JSON: template_path")
                self.run_ps_int(f'Copy-Item "{template_path}" "{vhd_path}"')
                vhd_created = True
                command += f'-VHDPath "{vhd_path}"; '

            self.run_ps_int(command)
            vm_created = True

            command = f'Remove-VMNetworkAdapter -VMName "{vm_name}" -Name "Network Adapter" -ErrorAction SilentlyContinue; '
            self.run_ps_int(command)

            command = f'Set-VMProcessor -VMName "{vm_name}" -Count "{cpus}"; '
            if (generation == 2):
                command += f'Set-VMFirmware -VMName "{vm_name}" -EnableSecureBoot Off; '

            self.run_ps_int(command)

            for idx, nic in enumerate(self.data["nics"]):
                adapter_name = f"NIC{idx+1}"
                self.run_ps_int(f'Add-VMNetworkAdapter -VMName "{vm_name}" -SwitchName "{network_switch}" -Name "{adapter_name}"')
                self.run_ps_int(f'Set-VMNetworkAdapter -VMName "{vm_name}" -Name "{adapter_name}" -StaticMacAddress "{nic["mac"]}"')
                self.run_ps_int(f'Set-VMNetworkAdapterVlan -VMName "{vm_name}" -VMNetworkAdapterName "{adapter_name}" -Access -VlanId "{nic["vlan"]}"')

            self.run_ps_int(f'Start-VM -Name "{vm_name}"')
            vm_started = True

            succeed({"status": "success", "message": "Instance created"})

        except Exception as e:
            if vm_started:
                self.run_ps_int(f'Stop-VM -Name "{vm_name}" -Force -TurnOff')
            if vm_created:
                self.run_ps_int(f'Remove-VM -Name "{vm_name}" -Force')
            if vhd_created:
                self.run_ps_int(f'Remove-Item -Path "{vhd_path}" -Force')
            fail(str(e))

    def start(self):
        self.run_ps(f'Start-VM -Name "{self.data["vmname"]}"')
        succeed({"status": "success", "message": "Instance started"})

    def stop(self):
        try:
            self.run_ps_int(f'Stop-VM -Name "{self.data["vmname"]}" -Force')
        except Exception as e:
            if self.vm_not_present(e):
                succeed({"status": "success", "message": "Instance stopped"})
            else:
                fail(str(e))
        succeed({"status": "success", "message": "Instance stopped"})

    def reboot(self):
        self.run_ps(f'Restart-VM -Name "{self.data["vmname"]}" -Force')
        succeed({"status": "success", "message": "Instance rebooted"})

    def status(self):
        command = f'(Get-VM -Name "{self.data["vmname"]}").State'
        state = self.run_ps(command)
        power_state = "unknown"
        if state.strip().lower() == "running":
            power_state = "poweron"
        elif state.strip().lower() == "off":
            power_state = "poweroff"
        succeed({"status": "success", "power_state": power_state})

    def delete(self):
        try:
            self.run_ps_int(f'Remove-VM -Name "{self.data["vmname"]}" -Force')
        except Exception as e:
            if self.vm_not_present(e):
                succeed({"status": "success", "message": "Instance deleted"})
            else:
                fail(str(e))
        succeed({"status": "success", "message": "Instance deleted"})

    def get_console(self):
        fail("Operation not supported")

    def suspend(self):
        self.run_ps(f'Suspend-VM -Name "{self.data["vmname"]}"')
        succeed({"status": "success", "message": "Instance suspended"})

    def resume(self):
        self.run_ps(f'Resume-VM -Name "{self.data["vmname"]}"')
        succeed({"status": "success", "message": "Instance resumed"})

    def create_snapshot(self):
        snapshot_name = self.data["snapshot_name"]
        if snapshot_name == "":
            fail("Missing required field in JSON: snapshot_name")
        command = f'Checkpoint-VM -VMName "{self.data["vmname"]}" -SnapshotName "{snapshot_name}"'
        self.run_ps(command)
        succeed({"status": "success", "message": f"Snapshot '{snapshot_name}' created"})

    def list_snapshots(self):
        command = (
            f'Get-VMSnapshot -VMName "{self.data["vmname"]}" '
            '| Select-Object Name, @{Name="CreationTime";Expression={$_.CreationTime.ToString("s")}} '
            '| ConvertTo-Json'
        )
        snapshots = json.loads(self.run_ps(command))
        succeed({"status": "success", "printmessage": "true", "message": snapshots})

    def restore_snapshot(self):
        snapshot_name = self.data["snapshot_name"]
        if snapshot_name == "":
            fail("Missing required field in JSON: snapshot_name")
        command = f'Restore-VMSnapshot -VMName "{self.data["vmname"]}" -Name "{snapshot_name}" -Confirm:$false'
        self.run_ps(command)
        succeed({"status": "success", "message": f"Snapshot '{snapshot_name}' restored"})

    def delete_snapshot(self):
        snapshot_name = self.data["snapshot_name"]
        if snapshot_name == "":
            fail("Missing required field in JSON: snapshot_name")
        command = f'Remove-VMSnapshot -VMName "{self.data["vmname"]}" -Name "{snapshot_name}" -Confirm:$false'
        self.run_ps(command)
        succeed({"status": "success", "message": f"Snapshot '{snapshot_name}' deleted"})


def main():
    if len(sys.argv) < 3:
        fail("Usage: script.py <operation> '<json-file-path>'")

    operation = sys.argv[1].lower()
    json_file_path = sys.argv[2]

    try:
        manager = HyperVManager(json_file_path)
    except FileNotFoundError:
        fail(f"JSON file not found: {json_file_path}")
    except json.JSONDecodeError:
        fail("Invalid JSON in file")

    operations = {
        "create": manager.create,
        "start": manager.start,
        "stop": manager.stop,
        "reboot": manager.reboot,
        "delete": manager.delete,
        "status": manager.status,
        "getconsole": manager.get_console,
        "suspend": manager.suspend,
        "resume": manager.resume,
        "listsnapshots": manager.list_snapshots,
        "createsnapshot": manager.create_snapshot,
        "restoresnapshot": manager.restore_snapshot,
        "deletesnapshot": manager.delete_snapshot
    }

    if operation not in operations:
        fail("Invalid action")

    try:
        operations[operation]()
    except Exception as e:
        fail(str(e))


if __name__ == "__main__":
    main()
