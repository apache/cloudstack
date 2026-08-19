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
