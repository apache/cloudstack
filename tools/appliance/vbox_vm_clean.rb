#!/usr/bin/env ruby
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

# script that tries hard to forcibly shut down all vms

# gem install sys-proctable
require 'sys/proctable'

include Sys

do_delete = (ARGV.include? 'delete' or ARGV.include? '--delete' or ARGV.include? '-d')
do_kill = (ARGV.include? 'kill' or ARGV.include? '--kill' or ARGV.include? '-k')

lines = `VBoxManage list vms`
vms = lines.split(/\n/)
if vms.nil?
  vms = []
end
vms.each do |vmline|
  vm_info = /\"(.*)\"[^{]*\{(.*)\}/.match(vmline)
  next if vm_info.nil?
  vm_name = vm_info[1]
  vm_uuid = vm_info[2]

  cmd="VBoxManage controlvm #{vm_name} poweroff"
  puts cmd
  `#{cmd}`
  if do_delete
    sleep(1)
    cmd="VBoxManage unregistervm #{vm_name} --delete"
    puts cmd
    `#{cmd}`
  end

  if do_kill
    sleep(1)
    # ps x | grep VBoxHeadless | grep systemvm64template-4.4.0 | egrep -o '^\s*[0-9]+' | xargs kill
    ProcTable.ps do |p|
      next unless p.cmdline.include? "VBoxHeadless"
      next unless p.cmdline.include? vm_name
      # not all rubies / proctables expose ruid
      if defined? p.ruid
        # VBoxManage should only list _our_ vms, but just to be safe...
        next unless p.ruid == Process.uid
      end

      puts "kill -SIGKILL #{p.pid}"
      begin
        Process.kill("KILL", p.pid)
      rescue => exception
        puts exception.backtrace
      end
      sleep(5)
      puts "kill -SIGTERM #{p.pid}"
      begin
        Process.kill("TERM", p.pid)
      rescue => exception
        puts exception.backtrace
      end
    end
  end
end
