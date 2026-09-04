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

"""Helpers shared by the Linstor plugin integration tests."""

import socket
import time

from marvin.codes import FAILED
from marvin.lib.common import get_template

# Registered by `cs-linstor.py setup-test-template`. Optional: clusters without it fall
# back to the builtin template, so nothing here requires the extra setup step.
TEST_TEMPLATE_NAME = "marvin-alpine"


def get_guest_template(api_client, zone_id, hypervisor="KVM", name=TEST_TEMPLATE_NAME):
    """The small purpose-built guest template if it is registered, else the builtin one.

    The builtin CentOS 5.5 template is an 8 GiB volume that takes ~55s to reach sshd.
    Worse, creating an *encrypted* volume from a template cannot share blocks with the
    source - a new passphrase means different ciphertext - so LINSTOR does a full
    block-level re-encrypt copy of the whole virtual size, measured at ~500s per VM.
    A ~512 MiB template makes that negligible.

    Note marvin's get_template() only returns FAILED when the list call itself comes back
    empty; otherwise it falls through to the first result even if nothing matched the
    requested type. So detection has to key off FAILED, and the type has to be USER
    because our template is registered rather than built in.
    """
    template = get_template(api_client, zone_id, template_filter="all",
                            template_type="USER", template_name=name,
                            hypervisor=hypervisor)
    if template != FAILED and getattr(template, "isready", False):
        return template
    return get_template(api_client, zone_id, hypervisor=hypervisor)


class ServiceReady:
    @classmethod
    def ready(cls, hostname: str, port: int) -> bool:
        try:
            s = socket.create_connection((hostname, port), timeout=1)
            s.close()
            return True
        except (ConnectionRefusedError, socket.timeout, OSError):
            return False

    @classmethod
    def wait(cls, hostname: str, port: int, wait_interval: float = 5, timeout: int = 120,
             service_name: str = 'ssh') -> bool:
        """
        Wait until the given service can be reached, raise RuntimeError on timeout.
        :param hostname: host to connect to
        :param port: port of the application
        :param wait_interval: seconds between connection attempts
        :param timeout: seconds to wait before raising
        :param service_name: name of the service waited for (used in the error message)
        """
        starttime = int(round(time.time() * 1000))
        while not cls.ready(hostname, port):
            if starttime + timeout * 1000 < int(round(time.time() * 1000)):
                raise RuntimeError("{s} {h} cannot be reached.".format(s=service_name, h=hostname))
            time.sleep(wait_interval)
        return True

    @classmethod
    def wait_ssh_ready(cls, hostname: str, wait_interval: float = 2, timeout: int = 120) -> bool:
        return cls.wait(hostname, 22, wait_interval, timeout, "ssh")
