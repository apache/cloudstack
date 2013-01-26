# -*- coding: utf-8 -*-
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

# Use following rules for versioning:
# <cloudstack major version>.<cloudstack minor version>.<cli increment>
__version__ = "4.0.0"

try:
    import os
    from precache import precached_verbs
except ImportError, e:
    precached_verbs = {}

# Add config key:value
config_file = os.path.expanduser('~/.cloudmonkey_config')
config_fields = {'host': 'localhost', 'port': '8080',
                 'protocol': 'http', 'path': '/client/api',
                 'apikey': '', 'secretkey': '',
                 'timeout': '3600', 'asyncblock': 'true',
                 'prompt': '🐵 cloudmonkey>', 'color': 'true',
                 'tabularize': 'false',
                 'log_file':
                 os.path.expanduser('~/.cloudmonkey_log'),
                 'history_file':
                 os.path.expanduser('~/.cloudmonkey_history')}

# Add verbs in grammar
grammar = ['create', 'list', 'delete', 'update', 'lock',
           'enable', 'activate', 'disable', 'add', 'remove',
           'attach', 'detach', 'associate', 'disassociate', 'generate', 'ldap',
           'assign', 'authorize', 'change', 'register', 'configure',
           'start', 'restart', 'reboot', 'stop', 'reconnect',
           'cancel', 'destroy', 'revoke', 'mark', 'reset',
           'copy', 'extract', 'migrate', 'restore', 'suspend',
           'get', 'query', 'prepare', 'deploy', 'upload']
