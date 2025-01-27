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

cookbook_file 'cloud-install-sys-tmplt' do
  action :create_if_missing
  mode 0755
  path node['cloudstack']['cloud-install-sys-tmplt']
end

cookbook_file 'createtmplt.sh' do
  action :create_if_missing
  mode 0755
  path node['cloudstack']['createtmplt']
end

cloudstack_system_template 'xenserver' do
  template_id '1'
  nfs_path node['cloudstack']['secondary']['path']
  nfs_server node['cloudstack']['secondary']['host']
  url node['cloudstack']['hypervisor_tpl']['xenserver']
  action :create
end
