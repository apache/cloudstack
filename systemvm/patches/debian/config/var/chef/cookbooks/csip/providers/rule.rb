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
# This provider manipulates ip rule sets
# eg.
# ip rule add fwmark 1 table Table_eth1
#
action :create do
  if @current_resource.exists
    Chef::Log.info "#{ @new_resource.dev } already exists - nothing to do."
  else
    converge_by("Creating rule for #{ @new_resource }") do
      createRule
    end
  end
end

action :delete do

end

def load_current_resource
  @current_resource = Chef::Resource::CsipRule.new(@new_resource.name)
  @current_resource.exists = false
  @current_resource.dev(@new_resource.dev)
  @current_resource.type(@new_resource.type)
  @current_resource.mask(@new_resource.mask)
  @current_resource.ip(@new_resource.ip)
  if @new_resource.type == "lookup"
      @current_resource.network(calculateNetwork(@new_resource.ip,@new_resource.mask))
      @current_resource.cidrm(calculateCIDRMask(@new_resource.mask))
  end
  if rule_exists?
     @current_resource.exists = true
  end
end

def rule_exists?
    # from 172.16.0.0/16 lookup
    # from all fwmark 0x1 lookup Table_eth1
    str = ""
    if @current_resource.type == "lookup"
       str = "from #{@current_resource.network}/#{@current_resource.cidrm} lookup"
    end
    tableNo = @current_resource.dev[3,1]
    if @current_resource.type == "fwmark"
       str = "from all fwmark 0x#{tableNo} lookup Table_#{current_resource.dev}"
    end
    executeReturn("ip rule show").each do |line|
       next if ! line.include? str
       return true
    end
    return false
end

def createRule
    if @current_resource.type == "fwmark"
        tableNo = @current_resource.dev[3,1].hex.to_s
        table = "Table_#{current_resource.dev}"
        execute("ip rule add fwmark #{tableNo} table #{table}")
    end
    return true
end
