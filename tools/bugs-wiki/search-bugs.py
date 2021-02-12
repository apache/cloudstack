#!/usr/bin/python
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

import imp
import os
import SOAPpy
import sys


conf = os.environ.get('BUGS_CONFIG_FILE', 'bugs.cfg')
cfg = imp.load_source('cfg', conf)
host = getattr(cfg, 'bugs_host', 'bugs.cloudstack.org')
user = cfg.bugs_user
passwd = cfg.bugs_pass

url = 'http://%(host)s/rpc/soap/jirasoapservice-v2?wsdl' % locals()
soap = SOAPpy.WSDL.Proxy(url)
auth = soap.login(user, passwd)


def doit(term):
  if not term:
    return
  issues = soap.getIssuesFromTextSearch(auth, term)
  if len(issues) == 0:
    return
  for issue in issues:
    print "Found %s in http://%s/browse/%s" % (term, host, issue['key'])


for term in sys.stdin.readlines():
  try:
    doit(term.strip())
  except Exception, exn:
    print exn
    pass
