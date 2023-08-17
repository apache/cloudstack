#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#  
#    http://www.apache.org/licenses/LICENSE-2.0
#  
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

default['cloudstack']['db']['host'] = '127.0.0.1'
default['cloudstack']['db']['user'] = 'cloud'
default['cloudstack']['db']['password'] = 'password'
default['cloudstack']['db']['rootusername'] = 'root'
default['cloudstack']['db']['rootpassword'] = 'cloud'
default['cloudstack']['db']['management_server_key'] = 'password'
default['cloudstack']['db']['database_key'] = 'password'
default['cloudstack']['db']['prefill'] = '/vagrant/prefill.sql'

default['cloudstack']['secondary']['host'] = node['ipaddress']
default['cloudstack']['secondary']['path'] = '/data/secondary'
default['cloudstack']['secondary']['mgt_path'] = node['cloudstack']['secondary']['path']

default['cloudstack']['primary']['host'] = node['ipaddress']
default['cloudstack']['primary']['path'] = '/data/primary'
default['cloudstack']['primary']['mgt_path'] = node['cloudstack']['primary']['path']


default['cloudstack']['configuration'] = '/vagrant/marvin.cfg.erb'
