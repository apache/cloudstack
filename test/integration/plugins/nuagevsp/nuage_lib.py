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
import functools

from marvin.cloudstackAPI import createSSHKeyPair, deleteSSHKeyPair


class MySSHKeyPair:
    """Manage SSH Key pairs"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, name=None, account=None,
               domainid=None, projectid=None):
        """Creates SSH keypair"""
        cmd = createSSHKeyPair.createSSHKeyPairCmd()
        cmd.name = name
        if account is not None:
            cmd.account = account
        if domainid is not None:
            cmd.domainid = domainid
        if projectid is not None:
            cmd.projectid = projectid
        return MySSHKeyPair(apiclient.createSSHKeyPair(cmd).__dict__)

    def delete(self, apiclient):
        """Delete SSH key pair"""
        cmd = deleteSSHKeyPair.deleteSSHKeyPairCmd()
        cmd.name = self.name
        cmd.account = self.account
        cmd.domainid = self.domainid
        apiclient.deleteSSHKeyPair(cmd)


class gherkin(object):
    """Decorator to mark a method as Gherkin style.
       Add extra colored logging
    """
    BLACK = "\033[0;30m"
    BLUE = "\033[0;34m"
    GREEN = "\033[0;32m"
    CYAN = "\033[0;36m"
    RED = "\033[0;31m"
    BOLDBLUE = "\033[1;34m"
    NORMAL = "\033[0m"

    def __init__(self, method):
        self.method = method

    def __get__(self, obj=None, objtype=None):
        @functools.wraps(self.method)
        def _wrapper(*args, **kwargs):
            gherkin_step = self.method.__name__.replace("_", " ").capitalize()
            obj.info("=G= %s%s%s" % (self.BOLDBLUE, gherkin_step, self.NORMAL))
            try:
                result = self.method(obj, *args, **kwargs)
                obj.info("=G= %s%s: [SUCCESS]%s" %
                         (self.GREEN, gherkin_step, self.NORMAL))
                return result
            except Exception as e:
                obj.info("=G= %s%s: [FAILED]%s%s" %
                         (self.RED, gherkin_step, self.NORMAL, e))
                raise
        return _wrapper