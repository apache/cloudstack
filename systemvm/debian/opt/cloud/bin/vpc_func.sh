#!/usr/bin/env bash
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

# @VERSION@

getEthByIp (){
  local ip=$1
  for dev in `ls -1 /sys/class/net | grep eth`
  do
    sudo ip addr show dev $dev | grep $ip\/ > /dev/null
    if [ $? -eq 0 ]
    then
      echo $dev
      return 0
    fi
  done
  return 1
}

getVPCcidr () {
  CMDLINE=$(cat /var/cache/cloud/cmdline)
  for i in $CMDLINE
  do
    # search for foo=bar pattern and cut out foo
    KEY=$(echo $i | cut -d= -f1)
    VALUE=$(echo $i | cut -d= -f2)
    if [ "$KEY" == "vpccidr" ]
    then
      echo "$VALUE"
      return 0
    fi
  done
  return 1
}

removeRulesForIp() {
  local ip=$1
  iptables-save -t mangle | grep $ip | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t mangle $rule
  done
  iptables-save -t nat | grep $ip | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t nat $rule
  done
  iptables-save -t filter | grep $ip | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t filter $rule
  done
}

# IPv6 usage counts, kept as named nftables counters by CsIpv6Usage.py. $1 is the counter prefix: "stats" for the
# global NETWORK_STATS chain, or a device such as eth2. get_ipv6_usage prints the bytes in the same order as the
# iptables usage rules (out, in, tcp_out, tcp_in), or nothing if the counters don't exist yet.
get_ipv6_usage () {
  nft list counters table ip6 cloudstack_usage 2> /dev/null | awk -v p="$1" '
    $1 == "counter" { name = $2 }
    $1 == "packets" { bytes[name] = $4 }
    END { if ((p "_out") in bytes) printf "%s:%s:%s:%s:", bytes[p "_out"], bytes[p "_in"], bytes[p "_tcp_out"], bytes[p "_tcp_in"] }'
}

reset_ipv6_usage () {
  nft -f - > /dev/null 2>&1 <<NFT
reset counter ip6 cloudstack_usage $1_out
reset counter ip6 cloudstack_usage $1_in
reset counter ip6 cloudstack_usage $1_tcp_out
reset counter ip6 cloudstack_usage $1_tcp_in
NFT
  return 0
}
