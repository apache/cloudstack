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
#   ip route flush cache
#
action :create do
  if @current_resource.exists
    Chef::Log.info "#{ @new_resource.dev } already exists - nothing to do."
  else
    converge_by("Creating route for #{ @new_resource }") do
      createRoute
    end
  end
end

action :delete do

end

def load_current_resource
  @current_resource = Chef::Resource::CsipRoute.new(@new_resource.name)
  @current_resource.exists = false
  @current_resource.ip(@new_resource.ip)
  @current_resource.mask(@new_resource.mask)
  @current_resource.dev(@new_resource.dev)
  @current_resource.table(@new_resource.table)
  @current_resource.type(@new_resource.type)
  @current_resource.network(calculateNetwork(@new_resource.ip,@new_resource.mask))
  @current_resource.cidrm(calculateCIDRMask(@new_resource.mask))
  if device_exists?
     @current_resource.exists = true
  end
end

def device_exists?
    Chef::Log.debug "Checking for existence of routing table"
    @current_resource.tableExists = checkTableExists
    Chef::Log.debug "Checking for existence of route"
    if @current_resource.type == "dev"
       @current_resource.routeExists = typeDevExists()
       return @current_resource.routeExists && @current_resource.tableExists
    end
    if @current_resource.type == "default"
       @current_resource.routeExists = typeDefaultExists()
       return @current_resource.routeExists && @current_resource.tableExists
    end
    Chef::Log.error "Cannot provision a route of type #{current_resource.type}"
    # Route cannot exist if the table does not but let us be belt and braces about this
    return true && @current_resource.tableExists
end

def checkTableExists
    file="/etc/iproute2/rt_tables"
    ::File.readlines(file).each do |line|
       next if line =~ /^#/
       next if ! line.include? "#{@current_resource.dev[3,1]} #{@current_resource.table}"
       return true
    end
    return false
end

def typeDevExists
    executeReturn("ip route show table #{@current_resource.table} dev #{current_resource.dev}").each do |line|
       next if ! line.include? "proto static"
       next if ! line.include? "#{current_resource.network}/#{current_resource.cidrm}"
       return true
    end
    return false
end

def typeDefaultExists
    executeReturn("ip route show table #{@current_resource.table} dev #{@current_resource.dev} via #{@current_resource.ip}").each do |line|
       next if ! line.include? "default"
       return true
    end
    return false
end

def createRoute 
    if(! @current_resource.tableExists)
       execute(" echo #{@current_resource.dev[3,1]} #{@current_resource.table} >> /etc/iproute2/rt_tables")
    end
    if(! @current_resource.routeExists)
       if(@current_resource.type == "dev")
          execute("ip route add #{current_resource.network}/#{current_resource.cidrm} dev #{current_resource.dev} table #{current_resource.table} proto static")
       end
       if(@current_resource.type == "default")
          execute("ip route add default via #{current_resource.ip} table #{current_resource.table} proto static")
       end
    end
end
