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
import shutil
import subprocess
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

import passwd_server_ip


class TestPasswdServerIp(unittest.TestCase):

    def multipart(self, fields, boundary="----boundary123"):
        body = b""
        for name, value in fields.items():
            body += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n" % (boundary, name)).encode()
            body += value.encode() + b"\r\n"
        body += ("--%s--\r\n" % boundary).encode()
        return "multipart/form-data; boundary=%s" % boundary, body

    def test_parse_multipart_form(self):
        contentType, body = self.multipart({"ip": "10.1.1.5", "password": "Ab1&=;'\"x y", "token": "abc"})
        form = passwd_server_ip.parseForm(contentType, body)
        self.assertEqual({"ip": "10.1.1.5", "password": "Ab1&=;'\"x y", "token": "abc"}, form)

    def test_parse_multipart_form_with_non_ascii_value(self):
        contentType, body = self.multipart({"password": "päss"})
        self.assertEqual("päss", passwd_server_ip.parseForm(contentType, body)["password"])

    def test_parse_urlencoded_form(self):
        form = passwd_server_ip.parseForm("application/x-www-form-urlencoded", b"ip=10.1.1.5&password=a%26b%3Dc&token=abc")
        self.assertEqual({"ip": "10.1.1.5", "password": "a&b=c", "token": "abc"}, form)

    def test_parse_form_without_fields(self):
        self.assertEqual({}, passwd_server_ip.parseForm(None, b""))
        contentType, body = self.multipart({})
        self.assertEqual({}, passwd_server_ip.parseForm(contentType, body))

    @unittest.skipUnless(shutil.which("curl"), "curl is not available")
    def test_parse_form_posted_by_curl(self):
        """ the same request as /opt/cloud/bin/savepassword.sh """
        received = {}

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                length = int(self.headers.get('content-length', 0))
                received.update(passwd_server_ip.parseForm(self.headers.get('content-type'), self.rfile.read(length)))
                self.send_response(200)
                self.end_headers()

            def log_message(self, format, *args):
                return

        server = HTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever)
        thread.start()
        try:
            subprocess.check_call(["curl", "-s", "--header", "DomU_Request: save_password",
                                   "http://127.0.0.1:%d/" % server.server_address[1],
                                   "-F", "ip=10.1.1.5", "-F", "password=Pa$$w&rd=1", "-F", "token=abc"])
        finally:
            server.shutdown()
            thread.join()
            server.server_close()
        self.assertEqual({"ip": "10.1.1.5", "password": "Pa$$w&rd=1", "token": "abc"}, received)


if __name__ == '__main__':
    unittest.main()
