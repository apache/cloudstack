#!/usr/bin/env python
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information#
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
###############################################################################
#
# Collect Jira issues from a filter and format the output for CHANGES.md
# Output content into console, does not write into file.
#
###############################################################################
"""jira.py: Output jira issues from https://issues.apache.org/jira into RST format for Apche CloudStack Release-Notes.

Usage:
  jira.py FILTERID -u USERNAME -p PASSWORD
  jira.py (-h | --help)
  jira.py --version

Options:
  -h --help     Show this screen.
  --version     Show version.

"""
from docopt import docopt
import requests
import json
import sys
import pprint


if __name__ == '__main__':
    arguments = docopt(__doc__, version='jira.py 2.0')

filterurl='https://issues.apache.org/jira/rest/api/2/filter/' + arguments['FILTERID']


r=requests.get(filterurl, auth=(arguments['USERNAME'],arguments['PASSWORD']))
rlist=r.json()['searchUrl']

get_all=requests.get(rlist, auth=(arguments['USERNAME'],arguments['PASSWORD'])).json()
count=get_all['total']

#print count
n, m = divmod(count, 50)

for i in range(n+1):

    issueslist=requests.get(rlist+'&startAt='+str(i*50), auth=(arguments['USERNAME'],arguments['PASSWORD'])).json()['issues']

    for issue in issueslist:
        '''assignee=issue['fields']['assignee']['displayName']
           reporter=issue['fields']['reporter']['displayName']
        '''
        print '['+ issue['key'] + '](https://issues.apache.org/jira/browse/' + issue['key'] + ') | ' + issue['fields']['summary'][:80] + '...'
