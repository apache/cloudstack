#!/usr/bin/python

import imp
import os
import SOAPpy
import sys


# Work around https://jira.atlassian.com/browse/CONF-6720.
def confluence_soap_parser(xml_str, rules=None,
                           parser=SOAPpy.Parser._parseSOAP):
    attribute = 'xsi:type="soapenc:Array"'
    xml_str = xml_str.replace('%s %s' % (attribute, attribute), attribute)
    return parser(xml_str, rules=rules)
SOAPpy.Parser._parseSOAP = confluence_soap_parser


conf = os.environ.get('WIKI_CONFIG_FILE', 'wiki.cfg')
cfg = imp.load_source('cfg', conf)
host = getattr(cfg, 'wiki_host', 'wiki.cloudstack.org')
user = cfg.wiki_user
passwd = cfg.wiki_pass

url = 'http://%(host)s/rpc/soap-axis/confluenceservice-v1?wsdl' % locals()
soap = SOAPpy.WSDL.Proxy(url)
auth = soap.login(user, passwd)


def doit(term):
  if not term:
    return
  pages = soap.search(auth, term, 10)
  if len(pages) == 0:
    return
  for page in pages:
    print "Found %s in %s" % (term, page['url'])
    print page['excerpt']
    print


for term in sys.stdin.readlines():
  try:
    doit(term.strip())
  except Exception, exn:
    print exn
    pass
