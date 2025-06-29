#!/usr/bin/env python
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

import paramiko
import json
import sys
import random


def parse_json(json_data):
    try:
        data = {}

        data["vmname"] = json_data["cloudstack.vm.details"]["name"] + "-" + json_data["virtualmachineid"]

        external_host_details = json_data["externaldetails"].get("host", [])
        data["url"] = external_host_details["url"]
        data["username"] = external_host_details["username"]
        data["password"] = external_host_details["password"]
        data["network_switch"] = external_host_details["network_switch"]
        data["default_vhd_path"] = external_host_details.get("default_vhd_path", "C:\\ProgramData\\Microsoft\\Windows\\Virtual Hard Disks")
        data["default_vm_path"] = external_host_details.get("default_vm_path", "C:\\ProgramData\\Microsoft\\Windows\\Hyper-V")

        external_vm_details = json_data["externaldetails"].get("virtualmachine", [])
        if external_vm_details:
            data["template_type"] = external_vm_details.get("template_type", "template")
            data["template_path"] = external_vm_details.get("template_path", "")
            data["vhd_size_gb"] = external_vm_details.get("vhd_size_gb", 25)
            data["iso_path"] = external_vm_details.get("iso_path", "")
            data["generation"] = external_vm_details.get("generation", 2)

        data["cpus"] = json_data["cloudstack.vm.details"].get("cpus", 2)
        data["memory"] = json_data["cloudstack.vm.details"].get("minRam", 536870912)

        nics = json_data["cloudstack.vm.details"].get("nics", [])
        data["nics"] = []
        for nic in nics:
            data["interfaces"].append({
                "mac": nic["mac_address"],
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


def fail(message):
    print(json.dumps({"error": message}))
    sys.exit(1)


def succeed(data):
    print(json.dumps(data))
    sys.exit(0)


def run_powershell_ssh_int(command, url, username, password):
    #print(f"[INFO] Connecting to {url} as {username}...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(url, username=username, password=password)

    ps_command = f'powershell -NoProfile -Command "{command.strip()}"'
    #print(f"[INFO] Executing: {ps_command}")
    stdin, stdout, stderr = ssh.exec_command(ps_command)

    output = stdout.read().decode().strip()
    error = stderr.read().decode().strip()
    ssh.close()

    if error:
        raise Exception(error)
    return output

def run_powershell_ssh(command, url, username, password):
    try:
        output = run_powershell_ssh_int(command, url, username, password)
        return output
    except Exception as e:
        fail(str(e))


def create(data):
    vm_name = data["vmname"]
    cpus = data["cpus"]
    memory = data["memory"]
    memory_mb = int(memory) / 1024 / 1024
    template_path = data["template_path"]
    vhd_path = data["default_vhd_path"] + "\\" + vm_name + ".vhdx"
    vhd_size_gb = data["vhd_size_gb"]
    generation = data["generation"]
    iso_path = data["iso_path"]
    switch_name = data["switch_name"]
    vm_path = data["default_vm_path"]
    template_type = data.get("template_type", "template")

    if (data["mac_address"] == ""):
        fail("Mac address not found")

    vhd_created = False
    vm_created = False
    vm_started = False
    try:
        command = (
            f'New-VM -Name \\"{vm_name}\\" -MemoryStartupBytes {memory_mb}MB '
            f'-Generation {generation}  -Path \\"{vm_path}\\" '
        )
        if template_type == "iso":
            if (iso_path == ""):
                fail("ISO path is required")
            command += (
                f'-NewVHDPath \\"{vhd_path}\\" -NewVHDSizeBytes {vhd_size_gb}GB; '
                f'Add-VMDvdDrive -VMName \\"{vm_name}\\" -Path \\"{iso_path}\\"; '
            )
        else:
            if (template_path == ""):
                fail("Template path is required")
            run_powershell_ssh_int(f'Copy-Item \\"{template_path}\\" \\"{vhd_path}\\"', data["url"], data["username"], data["password"])
            vhd_created = True
            command += f'-VHDPath \\"{vhd_path}\\"; '

        run_powershell_ssh_int(command, data["url"], data["username"], data["password"])
        vm_created = True

        command = (
            f'Set-VMProcessor -VMName \\"{vm_name}\\" -Count \\"{cpus}\\"; '
            f'Connect-VMNetworkAdapter -VMName \\"{vm_name}\\" -SwitchName \\"{switch_name}\\"; '
            f'Set-VMFirmware -VMName "{vm_name}" -EnableSecureBoot Off; '
        )
        run_powershell_ssh_int(command, data["url"], data["username"], data["password"])

        for nic in data["nics"]:
            run_powershell_ssh_int(f'Set-VMNetworkAdapter -VMName "{vm_name}" -StaticMacAddress "{nic["mac"]}"', data["url"], data["username"], data["password"])
            run_powershell_ssh_int(f'Set-VMNetworkAdapterVlan -VMName "{"vm_name"}" -Access -VlanId "{nic["vlan"]}"', data["url"], data["username"], data["password"])

        run_powershell_ssh_int(f'Start-VM -Name "{vm_name}"', data["url"], data["username"], data["password"])
        vm_started = True

        succeed({"status": "success", "message": "Instance created"})

    except Exception as e:
        if vm_started:
            run_powershell_ssh_int(f'Stop-VM -Name "{vm_name}" -Force -TurnOff', data["url"], data["username"], data["password"])
        if vm_created:
            run_powershell_ssh_int(f'Remove-VM -Name "{vm_name}" -Force', data["url"], data["username"], data["password"])
        if vhd_created:
            run_powershell_ssh_int(f'Remove-Item -Path \\"{vhd_path}\\" -Force', data["url"], data["username"], data["password"])
        fail(str(e))

def start(data):
    run_powershell_ssh(f'Start-VM -Name "{data["vmname"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance started"})


def stop(data):
    run_powershell_ssh(f'Stop-VM -Name "{data["vmname"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance stopped"})


def reboot(data):
    run_powershell_ssh(f'Restart-VM -Name "{data["vmname"]}" -Force', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance rebooted"})


def status(data):
    command = f'(Get-VM -Name "{data["vmname"]}").State'
    state = run_powershell_ssh(command, data["url"], data["username"], data["password"])
    if state.lower() == "running":
        power_state = "poweron"
    elif state.lower() == "off":
        power_state = "poweroff"
    else:
        power_state = "unknown"
    succeed({"status": "success", "power_state": power_state})


def delete(data):
    run_powershell_ssh(f'Remove-VM -Name "{data["vmname"]}" -Force', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance deleted"})


def suspend(data):
    run_powershell_ssh(f'Suspend-VM -Name "{data["vmname"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance suspended"})


def resume(data):
    run_powershell_ssh(f'Resume-VM -Name "{data["vmname"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance resumed"})


def create_snapshot(data):
    snapshot_name = data["snapshot_name"]
    if snapshot_name == "":
        fail("Missing snapshot_name in parameters")
    command = f'Checkpoint-VM -VMName \\"{data["vmname"]}\\" -SnapshotName \\"{snapshot_name}\\"'
    run_powershell_ssh(command, data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": f"Snapshot '{snapshot_name}' created"})


def restore_snapshot(data):
    snapshot_name = data["snapshot_name"]
    if snapshot_name == "":
        fail("Missing snapshot_name in parameters")
    command = f'Restore-VMSnapshot -VMName \\"{data["vmname"]}\\" -Name \\"{snapshot_name}\\" -Confirm:$false'
    run_powershell_ssh(command, data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": f"Snapshot '{snapshot_name}' restored"})


def delete_snapshot(data):
    snapshot_name = data["snapshot_name"]
    if snapshot_name == "":
        fail("Missing snapshot_name in parameters")
    command = f'Remove-VMSnapshot -VMName \\"{data["vmname"]}\\" -Name \\"{snapshot_name}\\" -Confirm:$false'
    run_powershell_ssh(command, data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": f"Snapshot '{snapshot_name}' deleted"})


def main():
    if len(sys.argv) < 3:
        fail("Usage: script.py <operation> '<json-file-path>'")

    operation = sys.argv[1].lower()
    json_file_path = sys.argv[2]

    try:
        with open(json_file_path, 'r') as f:
            json_data = json.load(f)
            data = parse_json(json_data)
    except FileNotFoundError:
        fail(f"JSON file not found: {json_file_path}")
    except json.JSONDecodeError:
        fail("Invalid JSON in file")

    operations = {
        "create": create,
        "start": start,
        "stop": stop,
        "reboot": reboot,
        "delete": delete,
        "status": status,
        "suspend": suspend,
        "resume": resume,
        "createsnapshot": create_snapshot,
        "restoresnapshot": restore_snapshot,
        "deletesnapshot": delete_snapshot
    }

    if operation not in operations:
        fail("Invalid action")

    operations[operation](data)


if __name__ == "__main__":
    main()
