#!/usr/bin/python

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
