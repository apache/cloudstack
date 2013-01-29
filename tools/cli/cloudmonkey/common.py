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
# <cloudstack version>-<cli increment, starts from 0>
__version__ = "4.1.0-0"

try:
    from os.path import expanduser
    import os
    from precache import precached_verbs
except ImportError, e:
    precached_verbs = {}

param_type = ['boolean', 'date', 'float', 'integer', 'short', 'list',
              'long', 'object', 'map', 'string', 'tzdate', 'uuid']

config_dir = expanduser('~/.cloudmonkey')
config_file = expanduser(config_dir + '/config')

# cloudmonkey config fields
config_fields = {'core': {}, 'ui': {}, 'server': {}, 'user': {}}

# core
config_fields['core']['cache_file'] = expanduser(config_dir + '/cache')
config_fields['core']['history_file'] = expanduser(config_dir + '/history')
config_fields['core']['log_file'] = expanduser(config_dir + '/log')

# ui
config_fields['ui']['color'] = 'true'
config_fields['ui']['prompt'] = '> '
config_fields['ui']['tabularize'] = 'false'

# server
config_fields['server']['asyncblock'] = 'true'
config_fields['server']['host'] = 'localhost'
config_fields['server']['path'] = '/client/api'
config_fields['server']['port'] = '8080'
config_fields['server']['protocol'] = 'http'
config_fields['server']['timeout'] = '3600'

# user
config_fields['user']['apikey'] = ''
config_fields['user']['secretkey'] = ''
