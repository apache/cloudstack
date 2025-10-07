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

import sys
import json
import time
from requests_oauthlib import OAuth1Session


def fail(message):
    print(json.dumps({"error": message}))
    sys.exit(1)


def succeed(data):
    print(json.dumps(data))
    sys.exit(0)


class MaasManager:
    def __init__(self, config_path):
        self.config_path = config_path
        self.data = self.parse_json()
        self.session = self.init_session()

    def parse_json(self):
        try:
            with open(self.config_path, "r") as f:
                json_data = json.load(f)

            extension = json_data.get("externaldetails", {}).get("extension", {})
            host = json_data.get("externaldetails", {}).get("host", {})
            vm = json_data.get("externaldetails", {}).get("virtualmachine", {})

            endpoint = host.get("endpoint") or extension.get("endpoint")
            apikey = host.get("apikey") or extension.get("apikey")

            details = json_data.get("cloudstack.vm.details", {}).get("details", {})

            os_name = details.get("os") or vm.get("os")
            architecture = details.get("architecture") or vm.get("architecture")
            distro_series = details.get("distro_series") or vm.get("distro_series")

            if not endpoint or not apikey:
                fail("Missing MAAS endpoint or apikey")

            if not endpoint.startswith("http://") and not endpoint.startswith("https://"):
                endpoint = "http://" + endpoint
            endpoint = endpoint.rstrip("/")

            parts = apikey.split(":")
            if len(parts) != 3:
                fail("Invalid apikey format. Expected consumer:token:secret")

            consumer, token, secret = parts

            system_id = details.get("maas_system_id") or vm.get("maas_system_id", "")

            vm_name = vm.get("vm_name") or json_data.get("cloudstack.vm.details", {}).get("name")
            if not vm_name:
                vm_name = f"cs-{system_id}" if system_id else "cs-unknown"

            return {
                "endpoint": endpoint,
                "consumer": consumer,
                "token": token,
                "secret": secret,
                "distro_series": distro_series or "ubuntu/focal",
                "os": os_name,
                "architecture": architecture,
                "system_id": system_id,
                "vm_name": vm_name,
            }
        except Exception as e:
            fail(f"Error parsing JSON: {str(e)}")

    def init_session(self):
        return OAuth1Session(
            self.data["consumer"],
            resource_owner_key=self.data["token"],
            resource_owner_secret=self.data["secret"],
        )

    def call_maas(self, method, path, data=None, ignore_404=False):
        if not path.startswith("/"):
            path = "/" + path
        url = f"{self.data['endpoint']}:5240/MAAS/api/2.0{path}"
        resp = self.session.request(method, url, data=data)

        if resp.status_code == 404 and ignore_404:
            return None

        if not resp.ok:
            fail(f"MAAS API error: {resp.status_code} {resp.text}")

        try:
            return resp.json() if resp.text else {}
        except ValueError:
            return {}

    def prepare(self):
        machines = self.call_maas("GET", "/machines/")
        ready = [m for m in machines if m.get("status_name") == "Ready"]
        if not ready:
            fail("No Ready machines available")

        sysid = self.data.get("system_id")

        if sysid:
            match = next((m for m in ready if m["system_id"] == sysid), None)
            if not match:
                fail(f"Provided system_id '{sysid}' not found among Ready machines")
            system = match
        else:
            system = ready[0]

        system_id = system["system_id"]
        mac = system.get("interface_set", [{}])[0].get("mac_address")
        hostname = system.get("hostname", "")

        if not mac:
            fail("No MAC address found")

        # Load original JSON so we can update nics
        with open(self.config_path, "r") as f:
            json_data = json.load(f)

        if json_data.get("cloudstack.vm.details", {}).get("nics"):
            json_data["cloudstack.vm.details"]["nics"][0]["mac"] = mac

        console_url = f"http://{self.data['endpoint'].replace('http://','').replace('https://','')}:5240/MAAS/r/machine/{system_id}/summary"

        result = {
            "nics": json_data["cloudstack.vm.details"]["nics"],
            "details": {
                "External:mac_address": mac,
                "External:maas_system_id": system_id,
                "External:hostname": hostname,
                "External:console_url": console_url,
            },
        }
        succeed(result)

    def create(self):
        sysid = self.data.get("system_id")
        if not sysid:
            fail("system_id missing for create")

        ds = self.data.get("distro_series", None)
        os_name = self.data.get("os")
        arch = self.data.get("architecture")

        deploy_payload = {"op": "deploy"}

        if os_name or arch:
            if os_name:
                deploy_payload["os"] = os_name
            if arch:
                deploy_payload["architecture"] = arch
            if ds:
                deploy_payload["distro_series"] = ds
        else:
            deploy_payload["distro_series"] = ds or "ubuntu/focal"

        deploy_payload["net-setup-method"] = "curtin"

        self.call_maas("POST", f"/machines/{sysid}/", deploy_payload)

        succeed({"status": "success", "message": "Instance created", "requested": deploy_payload})

    def delete(self):
        sysid = self.data.get("system_id")
        if not sysid:
            fail("system_id missing for delete")

        self.call_maas("POST", f"/machines/{sysid}/", {"op": "release"}, ignore_404=True)
        succeed({"status": "success", "message": f"Instance deleted or not found ({sysid})"})

    def start(self):
        sysid = self.data.get("system_id")
        if not sysid:
            fail("system_id missing for start")
        self.call_maas("POST", f"/machines/{sysid}/", {"op": "power_on"})
        succeed({"status": "success", "power_state": "PowerOn"})

    def stop(self):
        sysid = self.data.get("system_id")
        if not sysid:
            fail("system_id missing for stop")
        self.call_maas("POST", f"/machines/{sysid}/", {"op": "power_off"})
        succeed({"status": "success", "power_state": "PowerOff"})

    def reboot(self):
        sysid = self.data.get("system_id")
        if not sysid:
            fail("system_id missing for reboot")

        self.call_maas("POST", f"/machines/{sysid}/", {"op": "power_off"})
        time.sleep(5)
        self.call_maas("POST", f"/machines/{sysid}/", {"op": "power_on"})

        succeed({"status": "success", "power_state": "PowerOn", "message": "Reboot completed"})

    def status(self):
        sysid = self.data.get("system_id")
        if not sysid:
            fail("system_id missing for status")
        resp = self.call_maas("GET", f"/machines/{sysid}/")
        state = resp.get("power_state", "")
        if state == "on":
            mapped = "PowerOn"
        elif state == "off":
            mapped = "PowerOff"
        else:
            mapped = "PowerUnknown"
        succeed({"status": "success", "power_state": mapped})


def main():
    if len(sys.argv) < 3:
        fail("Usage: maas.py <action> <json-file-path>")

    action = sys.argv[1].lower()
    json_file = sys.argv[2]

    try:
        manager = MaasManager(json_file)
    except FileNotFoundError:
        fail(f"JSON file not found: {json_file}")

    actions = {
        "prepare": manager.prepare,
        "create": manager.create,
        "delete": manager.delete,
        "start": manager.start,
        "stop": manager.stop,
        "reboot": manager.reboot,
        "status": manager.status,
    }

    if action not in actions:
        fail("Invalid action")

    actions[action]()


if __name__ == "__main__":
    main()
