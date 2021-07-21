#!/bin/bash
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

PATH="/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin"

. /lib/lsb/init-functions

log_it() {
  echo "$(date) $@" >> /var/log/cloud.log
  log_action_msg "$@"
}

init_interfaces_orderby_macs() {
    macs=( $(echo $1 | sed "s/|/ /g") )
    total_nics=${#macs[@]}
    interface_file=${2:-"/etc/network/interfaces"}
    rule_file=${3:-"/etc/udev/rules.d/70-persistent-net.rules"}

    echo -n "auto lo" > $interface_file
    for((i=0; i<total_nics; i++))
    do
        if [[ $i < 3 ]]
        then
           echo -n " eth$i" >> $interface_file
        fi
    done

    cat >> $interface_file << EOF

iface lo inet loopback

EOF

    echo "" > $rule_file
    for((i=0; i < ${#macs[@]}; i++))
    do
        echo "SUBSYSTEM==\"net\", ACTION==\"add\", DRIVERS==\"?*\", ATTR{address}==\"${macs[$i]}\", NAME=\"eth$i\"" >> $rule_file
    done
}

init_interfaces() {
  if [ "$NIC_MACS" == "" ]
  then
    cat > /etc/network/interfaces << EOF
auto lo $1 $2 $3
iface lo inet loopback

EOF
  else
    init_interfaces_orderby_macs "$NIC_MACS"
  fi
}

setup_interface() {
  local intfnum=$1
  local ip=$2
  local mask=$3
  local gw=$4
  local force=$5
  local intf=eth${intfnum}
  local bootproto="static"

  if [ "$BOOTPROTO" == "dhcp" ]
  then
    if [ "$intfnum" != "0" ]
    then
       bootproto="dhcp"
    fi
  fi

  if [ "$ip" != "0.0.0.0" -a "$ip" != "" -o "$force" == "force" ]
  then
     echo "iface  $intf inet $bootproto" >> /etc/network/interfaces
     if [ "$bootproto" == "static" ]
     then
       echo "  address $ip " >> /etc/network/interfaces
       echo "  netmask $mask" >> /etc/network/interfaces
     fi
  fi

  if [ "$ip" == "0.0.0.0" -o "$ip" == "" ]
  then
      ifconfig $intf down
  fi

  if [ "$force" == "force" ]
  then
      ifdown $intf
  else
      ifdown $intf
      if [ "$RROUTER" != "1" -o "$1" != "2" ]
      then
          ifup $intf
      fi
  fi
}

setup_interface_ipv6() {
  sysctl net.ipv6.conf.all.disable_ipv6=0
  sysctl net.ipv6.conf.all.forwarding=1
  sysctl net.ipv6.conf.all.accept_ra=1

  sed  -i "s/net.ipv6.conf.all.disable_ipv6 =.*$/net.ipv6.conf.all.disable_ipv6 = 0/" /etc/sysctl.conf
  sed  -i "s/net.ipv6.conf.all.forwarding =.*$/net.ipv6.conf.all.forwarding = 1/" /etc/sysctl.conf
  sed  -i "s/net.ipv6.conf.all.accept_ra =.*$/net.ipv6.conf.all.accept_ra = 1/" /etc/sysctl.conf

  local intfnum=$1
  local ipv6="$2"
  local prelen="$3"
  local intf=eth${intfnum}

  echo "iface $intf inet6 static" >> /etc/network/interfaces
  echo "  address $ipv6 " >> /etc/network/interfaces
  echo "  netmask $prelen" >> /etc/network/interfaces
  echo "  accept_ra 1" >> /etc/network/interfaces
  ifdown $intf
  ifup $intf
}


enable_fwding() {
  local enabled=$1
  log_it "cloud: enable_fwding = $1"
  log_it "enable_fwding = $1"
  echo "$1" > /proc/sys/net/ipv4/ip_forward
  [ -f /etc/iptables/iptables.conf ] && sed  -i "s/ENABLE_ROUTING=.*$/ENABLE_ROUTING=$enabled/" /etc/iptables/iptables.conf && return
}

enable_passive_ftp() {
  log_it "cloud: enabling passive FTP for guest VMs"
  echo "$1" > /proc/sys/net/netfilter/nf_conntrack_helper
}

disable_rpfilter() {
  log_it "cloud: disable rp_filter"
  log_it "disable rpfilter"
  sed -i "s/net.ipv4.conf.default.rp_filter.*$/net.ipv4.conf.default.rp_filter = 0/" /etc/sysctl.conf
}

get_public_vif_list() {
  local vif_list=""
  for i in /sys/class/net/eth*; do
    vif=$(basename $i);
    if [ "$vif" != "eth0" ] && [ "$vif" != "eth1" ]
    then
      vif_list="$vif_list $vif";
    fi
  done

  echo $vif_list
}

disable_rpfilter_domR() {
  log_it "cloud: Tuning rp_filter on public interfaces"

  VIF_LIST=$(get_public_vif_list)
  log_it "rpfilter public interfaces :  $VIF_LIST"
  if [ "$DISABLE_RP_FILTER" == "true" ]
  then
      log_it "cloud: disable rp_filter on public interfaces"
      sed -i "s/net.ipv4.conf.default.rp_filter.*$/net.ipv4.conf.default.rp_filter = 0/" /etc/sysctl.conf
      echo "0" > /proc/sys/net/ipv4/conf/default/rp_filter
      for vif in $VIF_LIST; do
         log_it "cloud: disable rp_filter on public interface: $vif"
         sed -i "s/net.ipv4.conf.$vif.rp_filter.*$/net.ipv4.conf.$vif.rp_filter = 0/" /etc/sysctl.conf
         echo "0" > /proc/sys/net/ipv4/conf/$vif/rp_filter
      done
  else
      log_it "cloud: enable rp_filter on public interfaces"
      sed -i "s/net.ipv4.conf.default.rp_filter.*$/net.ipv4.conf.default.rp_filter = 1/" /etc/sysctl.conf
      echo "1" > /proc/sys/net/ipv4/conf/default/rp_filter
      for vif in $VIF_LIST; do
         log_it "cloud: enable rp_filter on public interface: $vif"
         sed -i "s/net.ipv4.conf.$vif.rp_filter.*$/net.ipv4.conf.$vif.rp_filter = 1/" /etc/sysctl.conf
         echo "1" > /proc/sys/net/ipv4/conf/$vif/rp_filter
      done
  fi
  log_it "cloud: Enabling rp_filter on Non-public interfaces(eth0,eth1,lo)"
  echo "1" > /proc/sys/net/ipv4/conf/eth0/rp_filter
  echo "1" > /proc/sys/net/ipv4/conf/eth1/rp_filter
  echo "1" > /proc/sys/net/ipv4/conf/lo/rp_filter
}

enable_irqbalance() {
  local enabled=$1
  local proc=0

  proc=$(cat /proc/cpuinfo | grep "processor" | wc -l)
  if [ $proc -le 1 ]  && [ $enabled -eq 1 ]
  then
    enabled=0
  fi

  log_it "Processors = $proc  Enable service ${svc} = $enabled"
  local cfg=/etc/default/irqbalance
  [ -f $cfg ] && sed  -i "s/ENABLED=.*$/ENABLED=$enabled/" $cfg && return
}

enable_vpc_rpsrfs() {
    local enable=$1
    if [ $enable -eq 0 ]
    then
        echo 0 > /etc/rpsrfsenable
    else
        echo 1 > /etc/rpsrfsenable
    fi

    return 0
}

enable_rpsrfs() {
  local enable=$1

  if [ $enable -eq 0 ]
  then
      echo 0 > /etc/rpsrfsenable
      return 0
  fi

  if [ ! -f /sys/class/net/eth0/queues/rx-0/rps_cpus ]
  then
      echo "rps is not enabled in the kernel"
      echo 0 > /etc/rpsrfsenable
      return 0
  fi

  proc=$(cat /proc/cpuinfo | grep "processor" | wc -l)
  if [ $proc -le 1 ]
  then
      echo 0 > /etc/rpsrfsenable
      return 0;
  fi

  echo 1 > /etc/rpsrfsenable
  num=1
  num=$(($num<<$proc))
  num=$(($num-1));
  echo $num;
  hex=$(printf "%x\n" $num)
  echo $hex;
  #enable rps
  echo $hex > /sys/class/net/eth0/queues/rx-0/rps_cpus
  echo $hex > /sys/class/net/eth2/queues/rx-0/rps_cpus

  #enble rfs
  echo 256 > /proc/sys/net/core/rps_sock_flow_entries
  echo 256 > /sys/class/net/eth0/queues/rx-0/rps_flow_cnt
  echo 256 > /sys/class/net/eth2/queues/rx-0/rps_flow_cnt
}

setup_common() {
  init_interfaces $1 $2 $3
  if [ -n "$ETH0_IP" ]
  then
    setup_interface "0" $ETH0_IP $ETH0_MASK $GW
  fi
  if [ -n "$ETH0_IP6" ]
  then
      setup_interface_ipv6 "0" $ETH0_IP6 $ETH0_IP6_PRELEN
  fi
  setup_interface "1" $ETH1_IP $ETH1_MASK $GW
  if [ -n "$ETH2_IP" ]
  then
    setup_interface "2" $ETH2_IP $ETH2_MASK $GW
  fi

  echo $NAME > /etc/hostname
  echo 'AVAHI_DAEMON_DETECT_LOCAL=0' > /etc/default/avahi-daemon
  hostnamectl set-hostname $NAME

  #Nameserver
  sed -i -e "/^nameserver.*$/d" /etc/resolv.conf # remove previous entries
  sed -i -e "/^nameserver.*$/d" /etc/dnsmasq-resolv.conf # remove previous entries
  if [ -n "$internalNS1" ]
  then
    echo "nameserver $internalNS1" > /etc/dnsmasq-resolv.conf
    echo "nameserver $internalNS1" > /etc/resolv.conf
  fi

  if [ -n "$internalNS2" ]
  then
    echo "nameserver $internalNS2" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $internalNS2" >> /etc/resolv.conf
  fi
  if [ -n "$NS1" ]
  then
    echo "nameserver $NS1" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $NS1" >> /etc/resolv.conf
  fi

  if [ -n "$NS2" ]
  then
    echo "nameserver $NS2" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $NS2" >> /etc/resolv.conf
  fi

  if [ -n "$IP6_NS1" ]
  then
    echo "nameserver $IP6_NS1" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $IP6_NS1" >> /etc/resolv.conf
  fi
  if [ -n "$IP6_NS2" ]
  then
    echo "nameserver $IP6_NS2" >> /etc/dnsmasq-resolv.conf
    echo "nameserver $IP6_NS2" >> /etc/resolv.conf
  fi

  if [ -n "$MGMTNET"  -a -n "$LOCAL_GW" ]
  then
    ip route add $MGMTNET via $LOCAL_GW dev eth1
  fi

  ip route delete default
  if [ "$RROUTER" != "1" ]
  then
    gwdev=$3
    if [ -z "$gwdev" ]
    then
      gwdev="eth0"
    fi

    ip route add default via $GW dev $gwdev
  fi

  # Workaround to activate vSwitch under VMware
  timeout 3 ping -n -c 3 $GW &
  if [ -n "$MGMTNET"  -a -n "$LOCAL_GW" ]
  then
      timeout 3 ping -n -c 3 $LOCAL_GW &
      #This code is added to address ARP issue by pinging MGMT_GW
      MGMT_GW=$(echo $MGMTNET | awk -F "." '{print $1"."$2"."$3".1"}')
      timeout 3 ping -n -c 3 $MGMT_GW &
  fi

  if [ "$HYPERVISOR" == "vmware" ]; then
      ntpq -p &> /dev/null || vmware-toolbox-cmd timesync enable
  fi
}

setup_dnsmasq() {
  log_it "Setting up dnsmasq"

  touch /etc/dhcpopts.txt

  [ -z $DHCP_RANGE ] && [ $ETH0_IP ] && DHCP_RANGE=$ETH0_IP
  [ $ETH0_IP6 ] && DHCP_RANGE_IP6=$ETH0_IP6
  [ -z $DOMAIN ] && DOMAIN="cloudnine.internal"
  #removing the dnsmasq multiple ranges config file.
  rm /etc/dnsmasq.d/multiple_ranges.conf

  #get the template
  cp /etc/dnsmasq.conf.tmpl /etc/dnsmasq.conf

  if [ -n "$DOMAIN" ]
  then
        #send domain name to dhcp clients
        sed -i s/[#]*dhcp-option=15.*$/dhcp-option=15,\"$DOMAIN\"/ /etc/dnsmasq.conf
        #DNS server will append $DOMAIN to local queries
        sed -r -i s/^[#]?domain=.*$/domain=$DOMAIN/ /etc/dnsmasq.conf
        #answer all local domain queries
        sed  -i -e "s/^[#]*local=.*$/local=\/$DOMAIN\//" /etc/dnsmasq.conf
  fi

  if [ -n  "$DNS_SEARCH_ORDER" ]
  then
      sed -i -e "/^[#]*dhcp-option.*=119.*$/d" /etc/dnsmasq.conf
      echo "dhcp-option-force=119,$DNS_SEARCH_ORDER" >> /etc/dnsmasq.conf
      # set the domain search order as a space seprated list for option 15
      DNS_SEARCH_ORDER=$(echo $DNS_SEARCH_ORDER | sed 's/,/ /g')
      #send domain name to dhcp clients
      sed -i s/[#]*dhcp-option=15.*$/dhcp-option=15,\""$DNS_SEARCH_ORDER"\"/ /etc/dnsmasq.conf
  fi

  if [ $DHCP_RANGE ]
  then
    sed -i -e "s/^dhcp-range_ip4=.*$/dhcp-range=$DHCP_RANGE,static/" /etc/dnsmasq.conf
  else
    sed -i -e "s/^dhcp-range_ip4=.*$//" /etc/dnsmasq.conf
  fi
  if [ $DHCP_RANGE_IP6 ]
  then
    sed -i -e "s/^dhcp-range_ip6=.*$/dhcp-range=$DHCP_RANGE_IP6,static/" /etc/dnsmasq.conf
    # For nondefault6 tagged host, don't send dns-server information
    sed -i /nondefault6/d /etc/dnsmasq.conf
    echo "dhcp-option=nondefault6,option6:dns-server" >> /etc/dnsmasq.conf
  else
    sed -i -e "s/^dhcp-range_ip6=.*$//" /etc/dnsmasq.conf
  fi

  if [ "$RROUTER" == "1" ]
  then
    DEFAULT_GW=$GUEST_GW
    INTERNAL_DNS=$GUEST_GW
  else
    if [ "$TYPE" == "dhcpsrvr" ]
    then
      DEFAULT_GW=$GW
    else
      DEFAULT_GW=$ETH0_IP
    fi
    INTERNAL_DNS=$ETH0_IP
  fi
  sed -i -e "/^[#]*dhcp-option=option:router.*$/d" /etc/dnsmasq.conf
  [ $DEFAULT_GW ] && echo "dhcp-option=option:router,$DEFAULT_GW" >> /etc/dnsmasq.conf

  [ $ETH0_IP ] && [ $NS1 ] && NS="$NS1,"
  [ $ETH0_IP ] && [ $NS2 ] && NS="$NS$NS2,"
  [ $ETH0_IP6 ] && [ $IP6_NS1 ] && NS6="[$IP6_NS1],"
  [ $ETH0_IP6 ] && [ $IP6_NS2 ] && NS6="$NS6[$IP6_NS2],"
  #for now set up ourself as the dns server as well
  sed -i -e "/^[#]*dhcp-option=6,.*$/d" /etc/dnsmasq.conf
  sed -i -e "/^[#]*dhcp-option=option6:dns-server,.*$/d" /etc/dnsmasq.conf
  if [ "$USE_EXTERNAL_DNS" != "true" ]
  then
    [ $ETH0_IP ] && NS="$INTERNAL_DNS,$NS"
    [ $ETH0_IP6 ] && NS6="[::],$NS6"
    # enable dns
    sed -i -e "/^[#]*port=.*$/d" /etc/dnsmasq.conf
  else
    # disable dns
    sed -i -e "/^[#]*port=.*$/d" /etc/dnsmasq.conf
    echo "port=0" >> /etc/dnsmasq.conf
  fi
  NS=${NS%?}
  NS6=${NS6%?}
  [ $ETH0_IP ] && echo "dhcp-option=6,$NS" >> /etc/dnsmasq.conf
  [ $ETH0_IP6 ] && echo "dhcp-option=option6:dns-server,$NS6" >> /etc/dnsmasq.conf
  #adding the name data-server to the /etc/hosts for allowing the access to user-data service and ssh-key reset in every subnet.
  #removing the existing entires to avoid duplicates on restarts.
  sed -i  '/data-server/d' /etc/hosts
  if [ -n "$ETH0_IP" ]
          then
           echo "$ETH0_IP data-server" >> /etc/hosts
  fi
  if [ -n "$ETH0_IP6" ]
      then
       echo "$ETH0_IP6 data-server" >> /etc/hosts
  fi
  #add the dhcp-client-update only if dnsmasq version is 2.6 and above
  dnsmasqVersion=$(dnsmasq -v |  grep version -m 1 | grep -o  "[[:digit:]]\.[[:digit:]]")
  major=$(echo "$dnsmasqVersion" | cut -d '.' -f 1)
  minor=$(echo "$dnsmasqVersion" | cut -d '.' -f 2)
  if [ "$major" -eq '2' -a  "$minor" -ge '6' ] || [ "$major" -gt '2' ]
  then
      sed -i -e "/^dhcp-client-update/d" /etc/dnsmasq.conf
      echo 'dhcp-client-update' >> /etc/dnsmasq.conf
  fi

  command -v dhcp_release > /dev/null 2>&1
  no_dhcp_release=$?
  if [ $no_dhcp_release -eq 0 -a -z "$ETH0_IP6" ]
  then
      echo 1 > /var/cache/cloud/dnsmasq_managed_lease
      sed -i -e "/^leasefile-ro/d" /etc/dnsmasq.conf
  else
      echo 0 > /var/cache/cloud/dnsmasq_managed_lease
  fi
}

setup_sshd(){
  local ip=$1
  local eth=$2
  [ -f /etc/ssh/sshd_config ] && sed -i -e "s/^[#]*ListenAddress.*$/ListenAddress $ip/" /etc/ssh/sshd_config
  sed -i "/3922/s/eth./$eth/" /etc/iptables/rules.v4
}

setup_vpc_apache2() {
  log_it "Setting up apache web server for VPC"
  systemctl disable apache2
  clean_ipalias_config
  setup_apache2_common
}

clean_ipalias_config() {
  rm -f /etc/apache2/conf.d/ports.*.meta-data.conf
  rm -f /etc/apache2/sites-available/ipAlias*
  rm -f /etc/apache2/sites-enabled/ipAlias*
  rm -f /etc/apache2/conf.d/vhost*.conf
  rm -f /etc/apache2/ports.conf
  rm -f /etc/apache2/vhostexample.conf
  rm -f /etc/apache2/sites-available/*
  rm -f /etc/apache2/sites-enabled/*

  rm -rf /etc/failure_config
}

setup_apache2_common() {
  sed -i 's/^Include ports.conf.*/# CS: Done by Python CsApp config\n#Include ports.conf/g' /etc/apache2/apache2.conf
  # Disable listing of http://SSVM-IP/icons folder for security issue. see article http://www.i-lateral.com/tutorials/disabling-the-icons-folder-on-an-ubuntu-web-server/
  [ -f /etc/apache2/mods-available/alias.conf ] && sed -i s/"Options Indexes MultiViews"/"Options -Indexes MultiViews"/ /etc/apache2/mods-available/alias.conf

  echo "Options -Indexes" > /var/www/html/.htaccess
}

setup_apache2() {
  log_it "Setting up apache web server"
  mkdir -p /var/www
  chown www-data:www-data -R /var/www
  clean_ipalias_config
  setup_apache2_common
  local ip=$1
}

setup_aesni() {
  if [ `grep aes /proc/cpuinfo | wc -l` -gt 0 ]
  then
    modprobe aesni_intel
  fi
}

setup_storage_network() {
    if [ x"$STORAGE_IP" == "x" -o x"$STORAGE_NETMASK" == "x" ]
    then
        log_it "Incompleted parameters STORAGE_IP:$STORAGE_IP, STORAGE_NETMASK:$STORAGE_NETMASK, STORAGE_CIDR:$STORAGE_CIDR. Cannot setup storage network"
        return
    fi

    echo "" >> /etc/network/interfaces
    echo "auto eth3" >> /etc/network/interfaces

    setup_interface "3" "$STORAGE_IP" "$STORAGE_NETMASK"
    [ -n "$MTU" ] && ifconfig eth3 mtu $MTU && echo "  mtu $MTU" >> /etc/network/interfaces
    #ip route add "$STORAGE_CIDR" via "$STORAGE_IP"
    log_it "Successfully setup storage network with STORAGE_IP:$STORAGE_IP, STORAGE_NETMASK:$STORAGE_NETMASK, STORAGE_CIDR:$STORAGE_CIDR"
}

setup_system_rfc1918_internal() {
  public_ip=`getPublicIp`
  echo "$public_ip" | grep -E "^((127\.)|(10\.)|(172\.1[6-9]\.)|(172\.2[0-9]\.)|(172\.3[0-1]\.)|(192\.168\.))"
  if [ "$?" == "0" ]; then
     log_it "Not setting up route of RFC1918 space to $LOCAL_GW befause $public_ip is RFC1918."
  else
     log_it "Setting up route of RFC1918 space to $LOCAL_GW"
     # Setup general route for RFC 1918 space, as otherwise it will be sent to
     # the public gateway and not work
     # More specific routes that may be set have preference over this generic route.
     ip route add 10.0.0.0/8 via $LOCAL_GW
     ip route add 172.16.0.0/12 via $LOCAL_GW
     ip route add 192.168.0.0/16 via $LOCAL_GW
  fi
}

getPublicIp() {
  public_ip=$ETH2_IP
  [ "$ETH2_IP" == "0.0.0.0" ] && public_ip=$ETH1_IP
  echo $public_ip
}

setup_ntp() {
    log_it "Setting up NTP"
    NTP_CONF_FILE="/etc/ntp.conf"
    if [ -f $NTP_CONF_FILE ]
    then
        IFS=',' read -a server_list <<< "$NTP_SERVER_LIST"
        sed -i "/^server /d" $NTP_CONF_FILE
        for (( iterator=${#server_list[@]}-1 ; iterator>=0 ; iterator-- ))
        do
            server=$(echo ${server_list[iterator]} | tr -d '\r')
            PATTERN="server $server"
            sed -i "0,/^#server/s//$PATTERN\n#server/" $NTP_CONF_FILE
        done
        systemctl enable ntp
    else
        log_it "NTP configuration file not found"
    fi
}

routing_svcs() {
   echo "haproxy apache2" > /var/cache/cloud/enabled_svcs
   echo "cloud nfs-common portmap" > /var/cache/cloud/disabled_svcs
   if [ "$RROUTER" -eq "1" ]
   then
       echo "keepalived conntrackd" >> /var/cache/cloud/enabled_svcs
       echo "dnsmasq" >> /var/cache/cloud/disabled_svcs
   else
       echo "dnsmasq" >> /var/cache/cloud/enabled_svcs
       echo "keepalived conntrackd " >> /var/cache/cloud/disabled_svcs
   fi
}

parse_cmd_line() {
  CMDLINE=$(cat /var/cache/cloud/cmdline)
  TYPE="unknown"
  BOOTPROTO="static"
  DISABLE_RP_FILTER="false"
  STORAGE_IP=""
  STORAGE_NETMASK=""
  STORAGE_CIDR=""
  VM_PASSWORD=""

  CHEF_TMP_FILE=/tmp/cmdline.json
  COMMA="\t"
  echo -e "{\n\"type\": \"cmdline\"," > ${CHEF_TMP_FILE}
  echo -e "\"cmd_line\": {" >> ${CHEF_TMP_FILE}

  for i in $CMDLINE
    do
      # search for foo=bar pattern and cut out foo
      KEY=$(echo $i | cut -d= -f1)
      VALUE=$(echo $i | cut -d= -f2)
      echo -en ${COMMA} >> ${CHEF_TMP_FILE}
      # Two lines so values do not accidently interpretted as escapes!!
      echo -n \"${KEY}\"': '\"${VALUE}\" >> ${CHEF_TMP_FILE}
      COMMA=",\n\t"
      case $KEY in
        disable_rp_filter)
            export DISABLE_RP_FILTER=$VALUE
            ;;
        eth0ip)
            export ETH0_IP=$VALUE
            ;;
        eth1ip)
            export ETH1_IP=$VALUE
            ;;
        eth2ip)
            export ETH2_IP=$VALUE
            ;;
        host)
            export MGMT_HOST=$VALUE
            ;;
        gateway)
            export GW=$VALUE
            ;;
        ip6gateway)
            export IP6GW=$VALUE
            ;;
        eth0mask)
            export ETH0_MASK=$VALUE
            ;;
        eth1mask)
            export ETH1_MASK=$VALUE
            ;;
        eth2mask)
            export ETH2_MASK=$VALUE
            ;;
        eth0ip6)
            export ETH0_IP6=$VALUE
            ;;
        eth0ip6prelen)
            export ETH0_IP6_PRELEN=$VALUE
            ;;
        internaldns1)
            export internalNS1=$VALUE
            ;;
        internaldns2)
            export internalNS2=$VALUE
            ;;
        dns1)
            export NS1=$VALUE
            ;;
        dns2)
            export NS2=$VALUE
            ;;
        ip6dns1)
            export IP6_NS1=$VALUE
            ;;
        ip6dns2)
            export IP6_NS2=$VALUE
            ;;
        domain)
            export DOMAIN=$VALUE
            ;;
        dnssearchorder)
            export DNS_SEARCH_ORDER=$VALUE
            ;;
        useextdns)
            export USE_EXTERNAL_DNS=$VALUE
            ;;
        mgmtcidr)
            export MGMTNET=$VALUE
            ;;
        localgw)
            export LOCAL_GW=$VALUE
            ;;
        template)
            export TEMPLATE=$VALUE
            ;;
        sshonguest)
            export SSHONGUEST=$VALUE
            ;;
        name)
            export NAME=$VALUE
            ;;
        dhcprange)
            export DHCP_RANGE=$(echo $VALUE | tr ':' ',')
            ;;
        bootproto)
            export BOOTPROTO=$VALUE
            ;;
        type)
            export TYPE=$VALUE
            ;;
        defaultroute)
            export DEFAULTROUTE=$VALUE
            ;;
        redundant_router)
            export RROUTER=$VALUE
            ;;
        redundant_state)
            export RROUTER_STATE=$VALUE
            ;;
        guestgw)
            export GUEST_GW=$VALUE
            ;;
        guestbrd)
            export GUEST_BRD=$VALUE
            ;;
        guestcidrsize)
            export GUEST_CIDR_SIZE=$VALUE
            ;;
        router_pr)
            export ROUTER_PR=$VALUE
            ;;
        extra_pubnics)
            export EXTRA_PUBNICS=$VALUE
            ;;
        nic_macs)
            export NIC_MACS=$VALUE
            ;;
        mtu)
            export MTU=$VALUE
            ;;
        storageip)
            export STORAGE_IP=$VALUE
            ;;
        storagenetmask)
            export STORAGE_NETMASK=$VALUE
            ;;
        storagecidr)
            export STORAGE_CIDR=$VALUE
            ;;
        vmpassword)
            export VM_PASSWORD=$VALUE
            ;;
        vpccidr)
            export VPCCIDR=$VALUE
            ;;
        cidrsize)
            export CIDR_SIZE=$VALUE
            ;;
        advert_int)
            export ADVERT_INT=$VALUE
            ;;
        ntpserverlist)
            export NTP_SERVER_LIST=$VALUE
            ;;
      esac
  done
  echo -e "\n\t}\n}" >> ${CHEF_TMP_FILE}
  if [ "$TYPE" != "unknown" ]
  then
    mv ${CHEF_TMP_FILE} /var/cache/cloud/cmd_line.json
  fi

  [ $ETH0_IP ] && export LOCAL_ADDRS=$ETH0_IP
  [ $ETH0_IP6 ] && export LOCAL_ADDRS=$ETH0_IP6
  [ $ETH0_IP ] && [ $ETH0_IP6 ] && export LOCAL_ADDRS="$ETH0_IP,$ETH0_IP6"

  # Randomize cloud password so only ssh login is allowed
  echo "cloud:`openssl rand -base64 32`" | chpasswd

  if [ x"$VM_PASSWORD" != x"" ]
  then
    echo "root:$VM_PASSWORD" | chpasswd
  fi
}

parse_cmd_line
