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

include_recipe 'cloudstack::marvin'
include_recipe 'cloudstack::management_server'

cloudstack_setup_database node['cloudstack']['db']['host'] do
  root_user node['cloudstack']['db']['rootusername']
  root_password node['cloudstack']['db']['rootpassword']
  user node['cloudstack']['db']['username']
  password node['cloudstack']['db']['password']
  action :create
end

cloudstack_prefill_database node['cloudstack']['db']['prefill'] do
  ip node['cloudstack']['db']['host']
  user node['cloudstack']['db']['username']
  password node['cloudstack']['db']['password']
end

cloudstack_system_template 'xenserver' do
  nfs_path node['cloudstack']['secondary']['path']
  nfs_server node['cloudstack']['secondary']['host']
  db_user node['cloudstack']['db']['username']
  url node['cloudstack']['hypervisor_tpl']['xenserver']
  db_password node['cloudstack']['db']['password']
  db_host node['cloudstack']['db']['host']
  action :create
end

cloudstack_setup_management node.name

service 'cloudstack-management' do
  action [:enable, :start]
end

cloudstack_configure_cloud node['cloudstack']['configuration'] do
  database_server_ip node['cloudstack']['db']['host']
  database_user node['cloudstack']['db']['username']
  database_password node['cloudstack']['db']['password']
end
