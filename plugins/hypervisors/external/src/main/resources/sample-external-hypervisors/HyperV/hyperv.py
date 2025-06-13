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


def run_powershell_ssh(command, host, username, password):
    try:
        print(f"[INFO] Connecting to {host} as {username}...")
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(host, username=username, password=password)

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
    vmdetails = data["vmdetails"]
    cpus = vmdetails.get("cpus", 2)
    memory = vmdetails.get("minRam", 536870912)
    vm_name = data["virtualmachinename"]
    memory_mb = int(memory) / 1024 / 1024 
    template_path = data.get("template_path", f"C:\\ProgramData\\Microsoft\\Windows\\Virtual Hard Disks\\m12-template.vhdx")
    vhd_path = data.get("vhd_path", f"C:\\ProgramData\\Microsoft\\Windows\\Virtual Hard Disks\\{vm_name}.vhdx")
    vhd_size_gb = data.get("vhd_size_gb", 40)
    generation = data.get("generation", 2)
    iso_path = data.get("iso_path", "C:\\Users\\Abhisar\\Downloads\\ubuntu-25.04-live-server-amd64.iso")
    switch_name = data.get("switch_name", "Default Switch")
    vm_path = data.get("vm_path", "C:\\ProgramData\\Microsoft\\Windows\\Hyper-V")
    template_type = data.get("template_type", "template")

    command = (
        f'New-VM -Name \\"{vm_name}\\" -MemoryStartupBytes {memory_mb}MB '
        f'-Generation {generation}  -Path \\"{vm_path}\\" '
    )
    if template_type == "iso":
        command += (
            f'-NewVHDPath \\"{vhd_path}\\" -NewVHDSizeBytes {vhd_size_gb}GB; '
            f'Add-VMDvdDrive -VMName \\"{vm_name}\\" -Path \\"{iso_path}\\"; '
        )
    else:
        run_powershell_ssh(f'Copy-Item \\"{template_path}\\" \\"{vhd_path}\\"', data["host"], data["username"], data["password"])
        command += f'-VHDPath \\"{vhd_path}\\"; '

    command += (
        f'Set-VMProcessor -VMName \\"{vm_name}\\" -Count \\"{cpus}\\"; '
        f'Connect-VMNetworkAdapter -VMName \\"{vm_name}\\" -SwitchName \\"{switch_name}\\"; '
        f'Set-VMNetworkAdapter -VMName "{vm_name}" -StaticMacAddress "{data["mac_address"]}"; '
        f'Set-VMFirmware -VMName "{vm_name}" -EnableSecureBoot Off; '
    )

    run_powershell_ssh(command, data["host"], data["username"], data["password"])

    # Switch should have vlan support (External/Internal)
    # run_powershell_ssh(f'Set-VMNetworkAdapterVlan -VMName "{"vm_name"}" -Access -VlanId "{data["cloudstack.vlan"]}"', data["host"], data["username"], data["password"])

    run_powershell_ssh(f'Start-VM -Name "{data["virtualmachinename"]}"', data["host"], data["username"], data["password"])

    succeed({"status": "success", "message": "Instance created"})


def start(data):
    run_powershell_ssh(f'Start-VM -Name "{data["virtualmachinename"]}"', data["host"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance started"})


def stop(data):
    run_powershell_ssh(f'Stop-VM -Name "{data["virtualmachinename"]}"', data["host"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance stopped"})


def reboot(data):
    run_powershell_ssh(f'Restart-VM -Name "{data["virtualmachinename"]}" -Force', data["host"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance rebooted"})


def pause(data):
    run_powershell_ssh(f'Suspend-VM -Name "{data["virtualmachinename"]}"', data["host"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance paused"})


def resume(data):
    run_powershell_ssh(f'Resume-VM -Name "{data["virtualmachinename"]}"', data["host"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance resumed"})


def delete(data):
    run_powershell_ssh(f'Remove-VM -Name "{data["virtualmachinename"]}" -Force', data["host"], data["username"], data["password"])
    succeed({"status": "success", "message": "Instance deleted"})


def status(data):
    command = f'(Get-VM -Name "{data["virtualmachinename"]}").State'
    state = run_powershell_ssh(command, data["host"], data["username"], data["password"])
    if state.lower() == "running":
        power_state = "poweron"
    elif state.lower() == "off":
        power_state = "poweroff"
    else:
        power_state = "unknown"
    succeed({"status": "success", "power_state": power_state})

def main():
    if len(sys.argv) < 3:
        fail("Usage: script.py <operation> '<json-args>'")

    operation = sys.argv[1].lower()

    try:
        data = json.loads(sys.argv[2])
        vmdetails = json.loads(data["cloudstack.vm.details"])
        data["vmdetails"] = vmdetails
    except json.JSONDecodeError:
        fail("Invalid JSON input")

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

