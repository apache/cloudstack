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
#
begin
    vr_ips = data_bag_item('vr', 'ips')
rescue
    raise format('Cannot find the %s databag item within the %s databag. Please correct this', 'vr', 'ips')
end

#begin
#    cmdline = data_bag_item('vr', 'cmdline')
#rescue
#    raise format('Cannot find the %s databag item within the %s databag. Please correct this', 'vr', 'cmdline')
#end

# List configured ips on this node and remove any that are not in the configuration
listIPs(vr_ips).each do |dev, ip|
    csip_device "#{dev}-#{ip}" do
        action :delete
        cidrs  ip
        index 0
        bdev dev
    end
end

vr_ips.each do |name,data|
  next unless data.class == Array
  next unless data.length > 0
  idx = 0
  data.each do |ipo|
      csip_device "#{name}-#{idx}" do
         object ipo
         index idx
         bdev name
      end
      idx += 1
  end
end

# Add an necessary routes
# This could be embedded in the device recipe is done like that for self healing purposes
vr_ips.each do |name,data|
  next unless data.class == Array
  next unless data.length > 0
  # ip route add $subnet/$mask dev $ethDev table $tableName proto static
  data.each do |ipo|
      csip_rule "#{name}-dev" do
         # ip rule add fwmark $tableNo table $tableName
         dev name
         type "fwmark"
      end
      csip_route "#{name}-dev" do
         type "dev"
         table "Table_#{name}"
         ip ipo['public_ip']
         mask ipo['netmask']
         dev name
      end
      csip_route "#{name}-default" do
         type "default"
         table "Table_#{name}"
         ip ipo['gateway']
         mask ipo['netmask']
         dev name
      end
  end
end
