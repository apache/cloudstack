#!/usr/bin/python
# $Id: hostvmstats.py 10054 2010-06-29 22:09:31Z abhishek $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/hostvmstats.py $

import XenAPI
import urllib
import time
import logging
logging.basicConfig(filename='/tmp/xapilog',level=logging.DEBUG)
                      
def get_stats(session, collect_host_stats, consolidation_function, interval, start_time):
  try:
    
    if collect_host_stats == "true" :
    	url = "http://localhost/rrd_updates?"
   	url += "session_id=" + session._session
   	url += "&host=" + collect_host_stats
    	url += "&cf=" + consolidation_function
    	url += "&interval=" + str(interval)
    	url += "&start=" + str(int(time.time())-100)
    else :
    	url = "http://localhost/rrd_updates?"
   	url += "session_id=" + session._session
   	url += "&host=" + collect_host_stats
    	url += "&cf=" + consolidation_function
    	url += "&interval=" + str(interval)
    	url += "&start=" + str(int(time.time())-100)

    logging.debug("Calling URL: %s",url)
    sock = urllib.URLopener().open(url)
    xml = sock.read()
    sock.close()
    logging.debug("Size of returned XML: %s",len(xml))
    return xml
  except Exception,e:
    logging.exception("get_stats() failed")
    raise
