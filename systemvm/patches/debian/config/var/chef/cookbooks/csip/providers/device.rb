require 'ipaddr'

action :create do
  if @current_resource.exists
    Chef::Log.info "#{ @new_resource.device } already configured - nothing to do."
  else
    converge_by("Setting up #{ @new_resource }") do
      plumbDevice
    end
  end
end

action :delete do
  if @current_resource.exists
    converge_by("Removing #{ @new_resource }") do
      unPlumbDevice
    end
  else
    Chef::Log.info "#{ @new_resource.device } not configured - nothing to do."
  end
end

def load_current_resource
  @current_resource = Chef::Resource::CsipDevice.new(@new_resource.name)
  @current_resource.index(@new_resource.index)
  @current_resource.name(@new_resource.name)
  @current_resource.bdev(@new_resource.bdev)
  @current_resource.object(@new_resource.object)
  @current_resource.exists = false
  if new_resource.cidrs.nil?
     @current_resource.cidrs(new_resource.object['publicIp'] + '/' + IPAddr.new( new_resource.object['vlanNetmask']).to_i.to_s(2).count("1").to_s)
  else
     @current_resource.cidrs(@new_resource.cidrs)
  end
  if device_exists?
     @current_resource.exists = true
  end
end

def device_exists?
    current_resource.device = current_resource.bdev
    if current_resource.index > 0
       current_resource.device = current_resource.device + ':' + current_resource.index.to_s
    end
    if not checkDevice
       Chef::Log.error "#{ current_resource.bdev } not present cannot configure"
       return true
    end 
    deviceUp?
    correctIP?
    correctConntrack?
    return current_resource.up && current_resource.configured && current_resource.contrack
end

def plumbDevice
    if ! current_resource.configured
       if ! execute("ip addr add dev #{current_resource.device} #{current_resource.cidrs} brd +")
          Chef::Log.error "#{ @new_resource.device } failed to configure ip on interface"
          return false
       end
    end
    if ! current_resource.up
       if ! execute("ip link set #{current_resource.device} up")
          Chef::Log.error "#{ @new_resource.device } failed to bring interface up"
          return false
       end
    end
    if ! current_resource.contrack
       if ! execute("iptables -t mangle -A PREROUTING -i #{current_resource.device} -m state --state NEW -j CONNMARK --set-mark #{current_resource.object['nicDevId']}")
          Chef::Log.error "#{ @new_resource.device } failed to set set conmark"
          return false
       end
    end
    execute("arping -c 1 -I #{current_resource.device} -A -U -s #{current_resource.object['publicIp']} #{current_resource.object['publicIp']}")
    execute("arping -c 1 -I #{current_resource.device} -A -U -s #{current_resource.object['publicIp']} #{current_resource.object['publicIp']}")
    return true
end

def unPlumbDevice
    pp "ip addr del dev #{current_resource.device} #{current_resource.cidrs}"
    if ! execute("ip addr del dev #{current_resource.device} #{current_resource.cidrs}")
         Chef::Log.error "#{ @new_resource.device } failed to delete ip on interface"
         return false
    end
    execute("ip route del table Table_#{current_resource.device}")
    return true
end

def correctConntrack?
    current_resource.contrack = execute("iptables-save -t mangle | grep  \"PREROUTING -i #{current_resource.bdev} -m state --state NEW -j CONNMARK --set-xmark\"")
    return current_resource.contrack
end

def correctIP?
    current_resource.configured = execute("ip addr show #{current_resource.bdev} | grep #{current_resource.cidrs}")
    return current_resource.configured
end

def deviceUp?
    current_resource.up = ! execute("ip link show #{current_resource.device} | grep DOWN")
    return current_resource.up
end

def checkDevice 
    file = ::File.open("/proc/net/dev")
    attempts = 0 
    found = false
    until attempts > 15 or found do
        ::File.readlines(file).each do |line|
           if line.include? "#{current_resource.bdev}:"
              found = true
           end
        end
        attempts += 1
        sleep(1)
    end
    file.close
    return found
end
