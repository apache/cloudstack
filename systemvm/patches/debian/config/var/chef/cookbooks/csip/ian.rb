require 'ipaddr'
require 'pp'

a = IPAddr.new("10.0.2.180")
pp a.mask("255.255.255.128").to_s
