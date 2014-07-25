# ----------------------------------------------------------- #
# Helper functions for the cookbook
#
# listIps will return a list of IPs/devices that should be
# deleted
# ----------------------------------------------------------- #
require 'ipaddr'

def listIPs(ips)
    # ----------------------------------------------------------- #
    # Collect all configured ip4 interfaces on the machine and
    # compare it to the cloudstack configuration
    # Returns a hash containing any ip/device combinations
    # that should not be there
    # ----------------------------------------------------------- #
    ipList = Hash.new
    cmd = Mixlib::ShellOut.new("ip addr show")
    cmd.run_command
    if cmd.exitstatus == 0
       cmd.stdout.each_line do |line|
           next unless line.include? "inet "
           bits = line.strip.split(/ /)
           # For now do not mess with the control interface
           next if bits[-1] == "lo" or bits[-1] == "eth0"
           if ! inConfig(ips, bits[-1], bits[1])
              ipList[ bits[-1] ] = bits[1]
           end
       end
    end
    return ipList
end

def inConfig(ips, dev, tip)
    # ----------------------------------------------------------- #
    #  Returns true if the ip/dev combination is in the config
    #  Returns false if it is not
    # ----------------------------------------------------------- #
    if ips[dev].nil?
       return false 
    end
    ips[dev].each do |o|
       oip = o['publicIp'] + '/' << IPAddr.new(o['vlanNetmask']).to_i.to_s(2).count("1").to_s
       if oip == tip
          return true
       end
    end
    return false
end

def execute(cmdStr)
  cmd = Mixlib::ShellOut.new("#{cmdStr}")
  cmd.run_command
  #puts "\n#{cmdStr} #{cmdPar} #{cmd.status}"
  cmd.exitstatus == 0
end

def executeReturn(cmdStr)
  cmd = Mixlib::ShellOut.new("#{cmdStr}")
  cmd.run_command
  #puts "\n#{cmdStr} #{cmdPar} #{cmd.status}"
  cmd.stdout.split(/\n/)
end

def calculateNetwork(ip, mask)
    return IPAddr.new(ip).mask(mask).to_s
end

def calculateCIDRMask(mask)
    return IPAddr.new( mask ).to_i.to_s(2).count("1").to_s
end
