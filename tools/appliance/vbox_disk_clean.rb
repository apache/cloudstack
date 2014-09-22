#!/usr/bin/env ruby

lines = `VBoxManage list hdds`
disks = lines.split(/\n\s*\n/)
disks.each do |disk|
  disk_lines = disk.split(/\n/)
  disk_config = {}
  disk_lines.each do |line|
    pair = line.split(/:\s*/)
    disk_config[pair[0]] = pair[1]
    # if pair[0] == 'Location'
    #   location = pair[1]

    #   if location.include? '/Snapshots/'
    #     disk_config['is_snapshot'] = true
    #   end
    #   if location.include? '/VirtualBox VMs/'
    #     disk_config['vm_name'] = location.split('/VirtualBox VMs/')[1].split('/')[0]
    #     disk_config['disk_name'] = location.split('/')[-1]
    #     disk_config['is_virtualbox_vm'] = true
    #   else
    #     disk_config['is_virtualbox_vm'] = false
    #     disk_config['disk_name'] = location.split('/')[-1]
    #   end
    # end
  end

  if disk_config.include? 'Location'
    cmd="VBoxManage closemedium disk '#{disk_config['Location']}' --delete"
    puts cmd
    `#{cmd}`
  end
end
