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

import patchviasocket

import getpass
import os
import socket
import tempfile
import time
import threading
import unittest

KEY_DATA = "I luv\nCloudStack\n"
CMD_DATA = "/run/this-for-me --please=TRUE! very%quickly"
NON_EXISTING_FILE = "must-not-exist"


def write_key_file():
    _, tmpfile = tempfile.mkstemp(".sck")
    with open(tmpfile, "w") as f:
        f.write(KEY_DATA)
    return tmpfile


class SocketThread(threading.Thread):
    def __init__(self):
        super(SocketThread, self).__init__()
        self._data = ""
        self._folder = tempfile.mkdtemp(".sck")
        self._file = os.path.join(self._folder, "socket")
        self._ready = False

    def data(self):
        return self._data

    def file(self):
        return self._file

    def wait_until_ready(self):
        while not self._ready:
            time.sleep(0.050)

    def run(self):
        TIMEOUT = 0.314     # Very short time for tests that don't write to socket.
        MAX_SIZE = 10 * 1024

        s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        try:
            s.bind(self._file)
            s.listen(1)
            s.settimeout(TIMEOUT)
            try:
                self._ready = True
                client, address = s.accept()
                self._data = client.recv(MAX_SIZE)
                client.close()
            except socket.timeout:
                pass
        finally:
            s.close()
            os.remove(self._file)
            os.rmdir(self._folder)


class TestPatchViaSocket(unittest.TestCase):
    def setUp(self):
        self._key_file = write_key_file()

        self._unreadable = write_key_file()
        os.chmod(self._unreadable, 0)

        self.assertFalse(os.path.exists(NON_EXISTING_FILE))
        self.assertNotEqual("root", getpass.getuser(), "must be non-root user (to test access denied errors)")

    def tearDown(self):
        os.remove(self._key_file)
        os.remove(self._unreadable)

    def test_write_to_socket(self):
        reader = SocketThread()
        reader.start()
        reader.wait_until_ready()
        self.assertEquals(0, patchviasocket.send_to_socket(reader.file(), self._key_file, CMD_DATA))
        reader.join()
        data = reader.data()
        self.assertIn(KEY_DATA, data)
        self.assertIn(CMD_DATA.replace("%", " "), data)
        self.assertNotIn("LUV", data)
        self.assertNotIn("very%quickly", data)  # Testing substitution

    def test_host_key_error(self):
        reader = SocketThread()
        reader.start()
        reader.wait_until_ready()
        self.assertEquals(1, patchviasocket.send_to_socket(reader.file(), NON_EXISTING_FILE, CMD_DATA))
        reader.join()   # timeout

    def test_host_key_access_denied(self):
        reader = SocketThread()
        reader.start()
        reader.wait_until_ready()
        self.assertEquals(1, patchviasocket.send_to_socket(reader.file(), self._unreadable, CMD_DATA))
        reader.join()   # timeout

    def test_nonexistant_socket_error(self):
        reader = SocketThread()
        reader.start()
        reader.wait_until_ready()
        self.assertEquals(1, patchviasocket.send_to_socket(NON_EXISTING_FILE, self._key_file, CMD_DATA))
        reader.join()   # timeout

    def test_invalid_socket_error(self):
        reader = SocketThread()
        reader.start()
        reader.wait_until_ready()
        self.assertEquals(1, patchviasocket.send_to_socket(self._key_file, self._key_file, CMD_DATA))
        reader.join()   # timeout

    def test_access_denied_socket_error(self):
        reader = SocketThread()
        reader.start()
        reader.wait_until_ready()
        self.assertEquals(1, patchviasocket.send_to_socket(self._unreadable, self._key_file, CMD_DATA))
        reader.join()   # timeout


if __name__ == '__main__':
    unittest.main()
