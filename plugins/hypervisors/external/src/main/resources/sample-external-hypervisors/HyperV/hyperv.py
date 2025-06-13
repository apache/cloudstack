#!/usr/bin/env python3

import paramiko
import json
import sys
import random

def fail(message):
    print(json.dumps({"error": message}))
    sys.exit(1)


def succeed(data):
    print(json.dumps(data))
    sys.exit(0)


def run_powershell_ssh(command, url, username, password):
    try:
        print(f"[INFO] Connecting to {url} as {username}...")
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(url, username=username, password=password)

        ps_command = f'powershell -NoProfile -Command "{command.strip()}"'
        print(f"[INFO] Executing: {ps_command}")
        stdin, stdout, stderr = ssh.exec_command(ps_command)

        output = stdout.read().decode().strip()
        error = stderr.read().decode().strip()
        ssh.close()

        if error:
            fail(error)
        else:
            return output
    except Exception as e:
        fail(str(e))


def generate_random_mac():
    hexchars = "0123456789ABCDEF"
    return "52:54:00:{:02X}:{:02X}:{:02X}".format(
        random.randint(0, 255),
        random.randint(0, 255),
        random.randint(0, 255)
    )


def prepare(data):
    mac_address = generate_random_mac()
    response = {
        "status": "success",
        "mac_address": mac_address,
        "message": "Instance prepared"
    }
    print(json.dumps(response))


def create(data):
    vm_name = data["virtualmachinename"]
    cpus = data["cpus"]
    memory = data["memory"]
    memory_mb = int(memory) / 1024 / 1024
    template_path = data["template_path"]
    #template_path = data.get("template_path", f"C:\\ProgramData\\Microsoft\\Windows\\Virtual Hard Disks\\m12-template.vhdx")
    vhd_path = data["default_vhd_path"] + "\\" + vm_name + ".vhdx"
    vhd_size_gb = data["vhd_size_gb"]
    generation = data["generation"]
    iso_path = data["iso_path"]
    #iso_path = data.get("iso_path", "C:\\Users\\Abhisar\\Downloads\\ubuntu-25.04-live-server-amd64.iso")
    switch_name = data.get["switch_name"]
    vm_path = data.get["vm_path"]
    template_type = data.get("template_type", "template")

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
        run_powershell_ssh(f'Copy-Item \\"{template_path}\\" \\"{vhd_path}\\"', data["url"], data["username"], data["password"])
        command += f'-VHDPath \\"{vhd_path}\\"; '

    command += (
        f'Set-VMProcessor -VMName \\"{vm_name}\\" -Count \\"{cpus}\\"; '
        f'Connect-VMNetworkAdapter -VMName \\"{vm_name}\\" -SwitchName \\"{switch_name}\\"; '
        f'Set-VMNetworkAdapter -VMName "{vm_name}" -StaticMacAddress "{data["mac_address"]}"; '
        f'Set-VMFirmware -VMName "{vm_name}" -EnableSecureBoot Off; '
    )

    run_powershell_ssh(command, data["url"], data["username"], data["password"])

    # Switch should have vlan support (External/Internal)
    # run_powershell_ssh(f'Set-VMNetworkAdapterVlan -VMName "{"vm_name"}" -Access -VlanId "{data["cloudstack.vlan"]}"', data["url"], data["username"], data["password"])

    run_powershell_ssh(f'Start-VM -Name "{data["virtualmachinename"]}"', data["url"], data["username"], data["password"])

    succeed({"status": "success", "message": "Instance created"})


def start(data):
    run_powershell_ssh(f'Start-VM -Name "{data["virtualmachinename"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance started"})


def stop(data):
    run_powershell_ssh(f'Stop-VM -Name "{data["virtualmachinename"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance stopped"})


def reboot(data):
    run_powershell_ssh(f'Restart-VM -Name "{data["virtualmachinename"]}" -Force', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance rebooted"})


def pause(data):
    run_powershell_ssh(f'Suspend-VM -Name "{data["virtualmachinename"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance paused"})


def resume(data):
    run_powershell_ssh(f'Resume-VM -Name "{data["virtualmachinename"]}"', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance resumed"})


def delete(data):
    run_powershell_ssh(f'Remove-VM -Name "{data["virtualmachinename"]}" -Force', data["url"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance deleted"})


def status(data):
    command = f'(Get-VM -Name "{data["virtualmachinename"]}").State'
    state = run_powershell_ssh(command, data["url"], data["username"], data["password"])
    if state.lower() == "running":
        power_state = "poweron"
    elif state.lower() == "off":
        power_state = "poweroff"
    else:
        power_state = "unknown"
    succeed({"status": "success", "power_state": power_state})

def parse_json(json_data):
    try:
        data = {
            "url": json_data["external"]["extensionid"]["url"],
            "username": json_data["external"]["extensionid"]["user"],
            "password": json_data["external"]["extensionid"]["secret"],
            "switch_name": json_data["external"]["hostid"].get("switch_name", "Default Switch"),
            "default_vhd_path": json_data["external"]["hostid"].get("default_vhd_path", "C:\\ProgramData\\Microsoft\\Windows\\Virtual Hard Disks"),
            "default_vm_path": json_data["external"]["hostid"].get("default_vm_path", "C:\\ProgramData\\Microsoft\\Windows\\Hyper-V"),
            "template_type": json_data["external"]["virtualmachineid"].get("template_type", "template"),
            "template_path": json_data["external"]["virtualmachineid"].get("template_path", ""),
            "vhd_size_gb": json_data["external"]["virtualmachineid"].get("vhd_size_gb", 25),
            "iso_path": json_data["external"]["virtualmachineid"].get("iso_path", ""),
            "generation": json_data["external"]["virtualmachineid"].get("generation", 2),
            "virtualmachinename": json_data["cloudstack.vm.details"]["name"],
            "cpus": json_data["cloudstack.vm.details"].get("cpus", 2),
            "memory": json_data["cloudstack.vm.details"].get("minRam", 536870912),
            "mac_address": json_data["cloudstack.vm.details"]["nics"][0]["mac"]
        }
        return data
    except KeyError as e:
        fail(f"Missing required field in JSON: {str(e)}")
    except Exception as e:
        fail(f"Error parsing JSON: {str(e)}")

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
        "prepare": prepare,
        "create": create,
        "start": start,
        "stop": stop,
        "reboot": reboot,
        "pause": pause,
        "resume": resume,
        "delete": delete,
        "status": status,
    }

    if operation not in operations:
        fail("Invalid action")

    operations[operation](data)


if __name__ == "__main__":
    main()

