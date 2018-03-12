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

import bambou
import importlib


class ApiClient(object):
    """
    This class provides utilities to instantiate an API client using vspk.
    Args:
        address (str): ip address or hostname where the VSD API is exposed.
        user (str): username to authenticate on the API.
        password (str): password to authenticate on the API.
        enterprise (str): VSD organization to use to authenticate on the API.
        version (str): version of the API to use.
    """

    def __init__(self, address, port='8443', user='csproot',
                 password='csproot', enterprise='csp', version=None):
        if not version:
            version = '5.0'
        self.url = 'https://{}:{}'.format(address, port)
        self.version = version
        self.user = user
        self.password = password
        self.enterprise = enterprise
        self.last_pushes = []
        self.session = None

    @staticmethod
    def import_vspk(version):
        """
        Return the vspk module corresponding to a given version of the API.
        Args:
            version (str): version of the API
        """
        version = 'v{}'.format(str(version).replace('.', '_'))
        try:
            vsdk = importlib.import_module('vspk.%s' % version)
        except:
            vsdk = importlib.import_module('vspk.vsdk.%s' % version)
        return vsdk

    def import_vsdenvs(self):
        """
        Return the root class a `vsdenvs`.
        """
        if not self.session:
            raise Exception('You must have an active session to use vsdenvs')
        self.vsdenvs = __import__('vsdenvs', globals(), locals(), [], -1)
        self.vsdenvs.NUCsprootEnvironment.instance = self.session.user

    def new_session(self):
        """
        Start a new API session via vspk an return the corresponding
        `vspk.NUVSDSession` object. Note that this object is also exposed as
        `self.session`
        """
        vspk = self.import_vspk(self.version)
        self.session = vspk.NUVSDSession(
            username=self.user,
            password=self.password,
            enterprise=self.enterprise,
            api_url=self.url)
        self.session.start()
        return self.session

    def start_push_center(self, callback=None):
        """
        Add a vspk push center to the current session.
        """
        if not callback:
            callback = self.default_callback
        self.session.push_center.add_delegate(callback)
        self.session.push_center.start()

    def stop_push_center(self, callback=None):
        """
        Stop the vpsk push center for the current session.
        """
        self.session.push_center.stop()

    def default_callback(self, data):
        """
        Default callback for the push center. It just stores the new event in
        a LILO queue exposed as `self.last_pushe`
        """
        self.last_pushes.append(data)
        # keep only the last 10 events
        if len(self.last_pushes) == 100:
            del self.last_pushes[-1]

    def __call__(self):
        if not self.session:
            return self.new_session()
        return self.session

    def add_license(self):
        """
        Add a license to the VSD
        """
        vspk = self.import_vspk(self.version)
        self.session.license = vspk.NULicense(license=self.license)
        try:
            self.session.user.create_child(self.session.license)
        except bambou.exceptions.BambouHTTPError:
            pass
        else:
            self.session.user.add_child(self.session.license)

    def delete_license(self):
        """
        Delete license on the VSD
        """
        self.session.user.licenses.fetch()
        for license in self.session.user.licenses:
            try:
                license.delete()
            except bambou.exceptions.BambouHTTPError:
                pass
