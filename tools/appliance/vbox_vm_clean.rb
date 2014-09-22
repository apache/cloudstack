#!/usr/bin/env ruby

# script that tries hard to forcibly shut down all vms

# gem install sys-proctable
require 'sys/proctable'

include Sys

do_delete = (ARGV.include? 'delete' or ARGV.include? '--delete' or ARGV.include? '-d')
do_kill = (ARGV.include? 'kill' or ARGV.include? '--kill' or ARGV.include? '-k')

lines = `VBoxManage list vms`
vms = lines.split(/\n/)
vms.each do |vmline|
  vm_info = /\"(.*)\"[^{]*\{(.*)\}/.match(vmline)
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
