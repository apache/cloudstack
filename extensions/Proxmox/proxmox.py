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

from __future__ import annotations
import datetime as _dt
import json
import re
import ssl
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib import error, parse, request

DEFAULT_WAIT_SECONDS = 600
PROXMOX_API_PREFIX = "/api2/json"
VM_NAME_PATTERN = re.compile(r"^[a-zA-Z0-9-]+$")


class ProxmoxError(RuntimeError):
    """Raised when the Proxmox API or payload validation fails."""


def fail(message: str) -> None:
    print(json.dumps({"status": "error", "error": message}))
    raise SystemExit(1)


def succeed(data: dict[str, Any]) -> None:
    print(json.dumps(data))
    raise SystemExit(0)


def _is_mapping(value: Any) -> bool:
    return isinstance(value, dict)


def _mapping(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _string(value: Any, default: str = "") -> str:
    if value is None:
        return default
    if isinstance(value, str):
        return value
    return str(value)


def _bool_text(value: Any, default: bool = False) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return _string(value).strip().lower() in {"1", "true", "yes", "on"}


def _int_text(value: Any, default: int = 0) -> int:
    if value is None or value == "":
        return default
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _normalize_url(url: str) -> str:
    url = url.strip()
    if not url.startswith(("http://", "https://")):
        url = "https://" + url
    return url.rstrip("/")


def _format_snapshot_time(value: Any) -> str:
    if value in (None, "", "-"):
        return "-"
    try:
        return _dt.datetime.fromtimestamp(int(float(value))).strftime(
            "%Y-%m-%d %H:%M:%S"
        )
    except (TypeError, ValueError, OSError, OverflowError):
        return _string(value, "-")


@dataclass(slots=True)
class ProxmoxSettings:
    url: str
    user: str
    token: str
    secret: str
    node: str
    network_bridge: str
    verify_tls_certificate: bool
    vm_name: str
    vm_internal_name: str
    vmid: str
    vmcpus: int
    vmmemory: int
    template_type: str
    template_id: str
    iso_path: str
    iso_os_type: str
    disk_size_gb: str
    storage: str
    is_full_clone: bool
    snap_name: str
    snap_description: str
    snap_save_memory: bool
    mac_addresses: list[str]
    vlans: list[str]


class ProxmoxManager:
    def __init__(self, config_path: str, wait_time: int | None = None):
        self.config_path = config_path
        self.wait_time = (
            wait_time if wait_time and wait_time > 0 else DEFAULT_WAIT_SECONDS
        )
        self.data = self.parse_json()
        self._ssl_context = (
            ssl.create_default_context()
            if self.data.verify_tls_certificate
            else ssl._create_unverified_context()  # noqa: SLF001 - intentional for admin-controlled TLS bypass
        )

    def parse_json(self) -> ProxmoxSettings:
        try:
            payload = json.loads(Path(self.config_path).read_text(encoding="utf-8"))
        except FileNotFoundError:
            fail(f"JSON file not found: {self.config_path}")
        except json.JSONDecodeError:
            fail("Invalid JSON in file")
        except OSError as exc:
            fail(f"Unable to read JSON file: {exc}")
        if not isinstance(payload, dict):
            fail("Invalid JSON input")
        externaldetails = _mapping(payload.get("externaldetails"))
        extension = _mapping(externaldetails.get("extension"))
        host = _mapping(externaldetails.get("host"))
        vm = _mapping(externaldetails.get("virtualmachine"))
        details_root = _mapping(payload.get("cloudstack.vm.details"))
        vm_details = _mapping(details_root.get("details"))
        parameters = _mapping(payload.get("parameters"))
        url = _string(host.get("url") or extension.get("url"))
        user = _string(host.get("user") or extension.get("user"))
        token = _string(host.get("token") or extension.get("token"))
        secret = _string(host.get("secret") or extension.get("secret"))
        node = _string(host.get("node"))
        network_bridge = _string(host.get("network_bridge"))
        verify_tls = _bool_text(host.get("verify_tls_certificate", "true"), True)
        vm_name = _string(vm.get("vm_name") or details_root.get("name"))
        vm_internal_name = _string(details_root.get("name"))
        vmid = _string(
            vm_details.get("proxmox_vmid")
            or details_root.get("details", {}).get("proxmox_vmid")
        )
        vmcpus = _int_text(details_root.get("cpus"), 0)
        vmmemory = _int_text(details_root.get("minRam"), 0)
        template_type = _string(vm.get("template_type"))
        template_id = _string(vm.get("template_id"))
        iso_path = _string(vm.get("iso_path"))
        iso_os_type = _string(vm.get("iso_os_type", "l26"))
        disk_size_gb = _string(vm.get("disk_size_gb", "64"))
        storage = _string(vm.get("storage", "local-lvm"))
        is_full_clone = _bool_text(vm.get("is_full_clone", "false"), False)
        snap_name = _string(parameters.get("snap_name"))
        snap_description = _string(parameters.get("snap_description"))
        snap_save_memory = _bool_text(parameters.get("snap_save_memory", False), False)
        nics = details_root.get("nics", [])
        if not isinstance(nics, list):
            nics = []
        mac_addresses: list[str] = []
        vlans: list[str] = []
        for nic in nics:
            nic_map = _mapping(nic)
            mac_addresses.append(_string(nic_map.get("mac")))
            vlan = _string(nic_map.get("broadcastUri"))
            vlans.append(vlan.removeprefix("vlan://"))
        if not url or not user or not token or not secret or not node:
            missing = [
                name
                for name, value in (
                    ("url", url),
                    ("user", user),
                    ("token", token),
                    ("secret", secret),
                    ("node", node),
                )
                if not value
            ]
            fail(f"Missing required fields: {' '.join(missing)}")
        return ProxmoxSettings(
            url=_normalize_url(url),
            user=user,
            token=token,
            secret=secret,
            node=node,
            network_bridge=network_bridge,
            verify_tls_certificate=verify_tls,
            vm_name=vm_name,
            vm_internal_name=vm_internal_name,
            vmid=vmid,
            vmcpus=vmcpus,
            vmmemory=vmmemory,
            template_type=template_type,
            template_id=template_id,
            iso_path=iso_path,
            iso_os_type=iso_os_type,
            disk_size_gb=disk_size_gb,
            storage=storage,
            is_full_clone=is_full_clone,
            snap_name=snap_name,
            snap_description=snap_description,
            snap_save_memory=snap_save_memory,
            mac_addresses=mac_addresses,
            vlans=vlans,
        )

    def _auth_header(self) -> str:
        return f"PVEAPIToken={self.data.user}!{self.data.token}={self.data.secret}"

    def call_api(
        self, method: str, path: str, data: dict[str, Any] | str | None = None
    ) -> dict[str, Any]:
        url = f"{self.data.url}{PROXMOX_API_PREFIX}{path}"
        headers = {"Authorization": self._auth_header()}
        body: bytes | None
        if data is None:
            body = None
        elif isinstance(data, str):
            body = data.encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            body = parse.urlencode(data, doseq=True).encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        req = request.Request(url, data=body, headers=headers, method=method.upper())
        try:
            with request.urlopen(req, context=self._ssl_context, timeout=120) as resp:
                raw = resp.read().decode("utf-8", errors="replace")
        except error.HTTPError as exc:
            raw = exc.read().decode("utf-8", errors="replace")
            raise ProxmoxError(
                self._extract_error_message(raw, exc.code, exc.reason)
            ) from exc
        except error.URLError as exc:
            raise ProxmoxError(str(getattr(exc, "reason", exc))) from exc
        if not raw.strip():
            return {}
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ProxmoxError(
                f"Invalid JSON response from Proxmox API: {raw}"
            ) from exc
        if not isinstance(parsed, dict):
            raise ProxmoxError("Invalid response from Proxmox API")
        return parsed

    @staticmethod
    def _extract_error_message(raw: str, status_code: int, reason: str | None) -> str:
        fallback = f"HTTP {status_code}{': ' + reason if reason else ''}"
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            return f"{fallback}: {raw.strip() or 'Unknown error'}"
        if isinstance(parsed, dict):
            for key in ("message", "error"):
                value = parsed.get(key)
                if value:
                    return _string(value)
            errors = parsed.get("errors")
            if errors:
                return _string(errors)
        return f"{fallback}: {raw.strip() or 'Unknown error'}"

    def execute_and_wait(
        self, method: str, path: str, data: dict[str, Any] | str | None = None
    ) -> None:
        response = self.call_api(method, path, data)
        upid = _string(response.get("data"))
        if not upid:
            message = _string(response.get("message"), "Unknown error")
            error_detail = _string(response.get("error"))
            if error_detail:
                message = error_detail
            raise ProxmoxError(
                f"Failed to execute API or retrieve UPID. Message: {message}"
            )
        self.wait_for_task(upid)

    def wait_for_task(
        self, upid: str, timeout: int | None = None, interval: int = 1
    ) -> None:
        timeout = self.wait_time if timeout is None or timeout <= 0 else timeout
        deadline = time.monotonic() + timeout
        task_path = f"/nodes/{self.data.node}/tasks/{parse.quote(upid, safe='')}/status"
        while True:
            if time.monotonic() > deadline:
                raise ProxmoxError("Timeout while waiting for async task")
            response = self.call_api("GET", task_path)
            status_data = _mapping(response.get("data"))
            task_status = _string(status_data.get("status")).lower()
            if task_status == "stopped":
                exit_status = _string(status_data.get("exitstatus"))
                if exit_status and exit_status != "OK":
                    raise ProxmoxError(f"Task failed with exit status: {exit_status}")
                return
            time.sleep(interval)

    def vm_not_present(self) -> bool:
        try:
            self.call_api(
                "GET", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/current"
            )
            return False
        except ProxmoxError as exc:
            message = str(exc).lower()
            return any(
                token in message
                for token in (
                    "not found",
                    "does not exist",
                    "no such vm",
                    "unknown vm",
                    "unable to find a virtual machine",
                    "vmid not found",
                )
            )

    def prepare(self) -> None:
        response = self.call_api("GET", "/cluster/nextid")
        vmid = _string(response.get("data"))
        if not vmid:
            raise ProxmoxError(
                _string(
                    response.get("message"), "Unable to retrieve next available VM ID"
                )
            )
        succeed({"details": {"proxmox_vmid": vmid}})

    def create(self) -> None:
        vm_name = self.data.vm_name or self.data.vm_internal_name
        if not vm_name:
            fail("Missing required fields: vm_internal_name")
        if not VM_NAME_PATTERN.fullmatch(vm_name):
            fail(
                f"Invalid VM name '{vm_name}'. Only alphanumeric characters and dashes (-) are allowed."
            )
        required = {
            "vmid": self.data.vmid,
            "network_bridge": self.data.network_bridge,
            "vmcpus": self.data.vmcpus,
            "vmmemory": self.data.vmmemory,
        }
        missing = [name for name, value in required.items() if value in (None, "", 0)]
        if missing:
            fail(f"Missing required fields: {' '.join(missing)}")
        cleanup_vm = False
        try:
            if self.data.template_type.strip().upper() == "ISO":
                if not self.data.iso_path:
                    fail("Missing required field in JSON: iso_path")
                if not self.data.disk_size_gb:
                    fail("Missing required field in JSON: disk_size_gb")
                payload = {
                    "vmid": self.data.vmid,
                    "name": vm_name,
                    "ide2": f"{self.data.iso_path},media=cdrom",
                    "ostype": self.data.iso_os_type,
                    "scsihw": "virtio-scsi-single",
                    "scsi0": f"{self.data.storage}:{self.data.disk_size_gb},iothread=on",
                    "sockets": 1,
                    "cores": self.data.vmcpus,
                    "numa": 0,
                    "cpu": "x86-64-v2-AES",
                    "memory": self.data.vmmemory // 1024 // 1024,
                }
                cleanup_vm = True
                self.execute_and_wait("POST", f"/nodes/{self.data.node}/qemu/", payload)
            else:
                if not self.data.template_id:
                    fail("Missing required field in JSON: template_id")
                clone_payload = {
                    "newid": self.data.vmid,
                    "name": vm_name,
                    "storage": self.data.storage,
                    "full": 1 if self.data.is_full_clone else 0,
                }
                cleanup_vm = True
                self.execute_and_wait(
                    "POST",
                    f"/nodes/{self.data.node}/qemu/{self.data.template_id}/clone",
                    clone_payload,
                )
                config_payload = {
                    "cores": self.data.vmcpus,
                    "memory": self.data.vmmemory // 1024 // 1024,
                }
                self.execute_and_wait(
                    "POST",
                    f"/nodes/{self.data.node}/qemu/{self.data.vmid}/config",
                    config_payload,
                )
            for idx, (mac, vlan) in enumerate(
                zip(self.data.mac_addresses, self.data.vlans, strict=False)
            ):
                if not mac or not vlan:
                    continue
                network_value = f"virtio={mac},bridge={self.data.network_bridge},tag={vlan},firewall=0"
                self.call_api(
                    "PUT",
                    f"/nodes/{self.data.node}/qemu/{self.data.vmid}/config/",
                    {f"net{idx}": network_value},
                )
            self.execute_and_wait(
                "POST", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/start"
            )
            cleanup_vm = False
            succeed({"status": "success", "message": "Instance created"})
        except ProxmoxError:
            if cleanup_vm:
                self._cleanup_created_vm()
            raise
        except Exception as exc:
            if cleanup_vm:
                self._cleanup_created_vm()
            raise ProxmoxError(str(exc)) from exc

    def _cleanup_created_vm(self) -> None:
        try:
            self.call_api("DELETE", f"/nodes/{self.data.node}/qemu/{self.data.vmid}")
        except Exception:
            pass

    def start(self) -> None:
        self.execute_and_wait(
            "POST", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/start"
        )
        succeed({"status": "success", "message": "Instance started"})

    def stop(self) -> None:
        if self.vm_not_present():
            succeed({"status": "success", "message": "Instance stopped"})
        self.execute_and_wait(
            "POST", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/stop"
        )
        succeed({"status": "success", "message": "Instance stopped"})

    def reboot(self) -> None:
        self.execute_and_wait(
            "POST", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/reboot"
        )
        succeed({"status": "success", "message": "Instance rebooted"})

    def status(self) -> None:
        response = self.call_api(
            "GET", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/current"
        )
        vm_status = _string(_mapping(response.get("data")).get("status")).lower()
        power_state = {
            "running": "poweron",
            "stopped": "poweroff",
        }.get(vm_status, "unknown")
        succeed({"status": "success", "power_state": power_state})

    def statuses(self) -> None:
        response = self.call_api("GET", f"/nodes/{self.data.node}/qemu")
        vms = response.get("data")
        if vms in (None, "", "null"):
            vms = []
        if isinstance(vms, dict):
            vms = [vms]
        if not isinstance(vms, list):
            fail("Failed to parse VM status output")
        power_state: dict[str, str] = {}
        for vm in vms:
            vm_map = _mapping(vm)
            if vm_map.get("template") == 1:
                continue
            name = _string(vm_map.get("name") or vm_map.get("vmid"))
            status = _string(vm_map.get("status")).lower()
            power_state[name] = {"running": "poweron", "stopped": "poweroff"}.get(
                status, "unknown"
            )
        succeed({"status": "success", "power_state": power_state})

    def delete(self) -> None:
        if self.vm_not_present():
            succeed({"status": "success", "message": "Instance deleted"})
        self.execute_and_wait(
            "DELETE", f"/nodes/{self.data.node}/qemu/{self.data.vmid}"
        )
        succeed({"status": "success", "message": "Instance deleted"})

    def get_console(self) -> None:
        response = self.call_api(
            "POST", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/vncproxy"
        )
        data = _mapping(response.get("data"))
        port = _string(data.get("port"))
        ticket = _string(data.get("ticket"))
        if not port or not ticket:
            raise ProxmoxError("Proxmox response missing port/ticket")
        host = self.get_node_host()
        if not host:
            raise ProxmoxError(f"Could not determine host IP for node {self.data.node}")
        succeed(
            {
                "status": "success",
                "message": "Console retrieved",
                "console": {
                    "host": host,
                    "port": port,
                    "password": ticket,
                    "passwordonetimeuseonly": True,
                    "protocol": "vnc",
                },
            }
        )

    def get_node_host(self) -> str:
        try:
            response = self.call_api("GET", f"/nodes/{self.data.node}/network")
        except ProxmoxError:
            return ""
        net_json = response.get("data", [])
        if not isinstance(net_json, list):
            return ""

        def _first_ip(entries: list[dict[str, Any]], allow_non_bridge: bool) -> str:
            for entry in entries:
                entry_map = _mapping(entry)
                entry_type = _string(entry_map.get("type")).lower()
                method = _string(entry_map.get("method")).lower()
                address = _string(entry_map.get("address"))
                cidr = _string(entry_map.get("cidr"))
                if allow_non_bridge and entry_type in {"bridge", "bond"}:
                    continue
                if allow_non_bridge and method != "static":
                    continue
                if address:
                    return address
                if cidr:
                    return cidr.split("/")[0]
            return ""

        host = _first_ip(net_json, allow_non_bridge=True)
        if not host:
            host = _first_ip(net_json, allow_non_bridge=False)
        return host

    def list_snapshots(self) -> None:
        response = self.call_api(
            "GET", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/snapshot"
        )
        snapshots = response.get("data", [])
        if snapshots in (None, "", "null"):
            snapshots = []
        if isinstance(snapshots, dict):
            snapshots = [snapshots]
        if not isinstance(snapshots, list):
            raise ProxmoxError("Failed to parse snapshot output")
        formatted = []
        for snapshot in snapshots:
            snap_map = _mapping(snapshot)
            formatted.append(
                {
                    "name": _string(snap_map.get("name")),
                    "snaptime": _format_snapshot_time(snap_map.get("snaptime")),
                    "description": snap_map.get("description"),
                    "parent": _string(snap_map.get("parent") or "-"),
                    "vmstate": _string(snap_map.get("vmstate") or "-"),
                }
            )
        succeed({"status": "success", "printmessage": "true", "message": formatted})

    def create_snapshot(self) -> None:
        if not self.data.snap_name:
            fail("Missing required field in JSON: snap_name")
        if not VM_NAME_PATTERN.fullmatch(self.data.snap_name):
            fail(
                f"Invalid Snapshot name '{self.data.snap_name}'. Only alphanumeric characters and dashes (-) are allowed."
            )
        payload = {
            "snapname": self.data.snap_name,
            "vmstate": 1 if self.data.snap_save_memory else 0,
        }
        if self.data.snap_description:
            payload["description"] = self.data.snap_description
        self.execute_and_wait(
            "POST", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/snapshot", payload
        )
        succeed({"status": "success", "message": "Instance Snapshot created"})

    def restore_snapshot(self) -> None:
        if not self.data.snap_name:
            fail("Missing required field in JSON: snap_name")
        if not VM_NAME_PATTERN.fullmatch(self.data.snap_name):
            fail(
                f"Invalid Snapshot name '{self.data.snap_name}'. Only alphanumeric characters and dashes (-) are allowed."
            )
        self.execute_and_wait(
            "POST",
            f"/nodes/{self.data.node}/qemu/{self.data.vmid}/snapshot/{self.data.snap_name}/rollback",
        )
        try:
            status_response = self.call_api(
                "GET", f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/current"
            )
            vm_status = _string(
                _mapping(status_response.get("data")).get("status")
            ).lower()
            if vm_status == "stopped":
                self.execute_and_wait(
                    "POST",
                    f"/nodes/{self.data.node}/qemu/{self.data.vmid}/status/start",
                )
        except ProxmoxError:
            pass
        succeed({"status": "success", "message": "Instance Snapshot restored"})

    def delete_snapshot(self) -> None:
        if not self.data.snap_name:
            fail("Missing required field in JSON: snap_name")
        if not VM_NAME_PATTERN.fullmatch(self.data.snap_name):
            fail(
                f"Invalid Snapshot name '{self.data.snap_name}'. Only alphanumeric characters and dashes (-) are allowed."
            )
        self.execute_and_wait(
            "DELETE",
            f"/nodes/{self.data.node}/qemu/{self.data.vmid}/snapshot/{self.data.snap_name}",
        )
        succeed({"status": "success", "message": "Instance Snapshot deleted"})


def main() -> None:
    if len(sys.argv) < 3:
        fail("Usage: proxmox.py <operation> '<json-file-path>'")
    action = sys.argv[1].lower()
    json_file_path = sys.argv[2]
    wait_time = (
        _int_text(sys.argv[3], DEFAULT_WAIT_SECONDS)
        if len(sys.argv) > 3
        else DEFAULT_WAIT_SECONDS
    )
    try:
        manager = ProxmoxManager(json_file_path, wait_time=wait_time)
    except ProxmoxError as exc:
        fail(str(exc))

    operations = {
        "prepare": manager.prepare,
        "create": manager.create,
        "start": manager.start,
        "stop": manager.stop,
        "reboot": manager.reboot,
        "delete": manager.delete,
        "status": manager.status,
        "statuses": manager.statuses,
        "getconsole": manager.get_console,
        "listsnapshots": manager.list_snapshots,
        "createsnapshot": manager.create_snapshot,
        "restoresnapshot": manager.restore_snapshot,
        "deletesnapshot": manager.delete_snapshot,
    }
    operation = operations.get(action)
    if operation is None:
        fail("Invalid action")

    try:
        operation()
    except ProxmoxError as exc:
        fail(str(exc))
    except SystemExit:
        raise
    except Exception as exc:
        fail(str(exc))


if __name__ == "__main__":
    main()
