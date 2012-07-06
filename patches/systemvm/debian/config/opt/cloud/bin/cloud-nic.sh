#!/usr/bin/env bash


plug_nic() {
  sudo echo "$tableNo $tableName" >> /etc/iproute2/rt_tables 2>/dev/null
  sudo ip rule add fwmark $tableNo table $tableName 2>/dev/null
  sudo ip route flush table $tableName
  sudo ip route flush cache
}


unplug_nic() {
  sudo iptables -t mangle -D PREROUTING -i $dev -m state --state NEW -j CONNMARK --set-mark $tableNo 2>/dev/null

  sudo ip rule del fwmark $tableNo 2>/dev/null
  sudo ip route flush table $tableName
  sudo sed -i /"$tableNo $tableName"/d /etc/iproute2/rt_tables 2>/dev/null
  sudo ip route flush cache
  # remove rules
  sudo iptables -t mangle -F NETWORK_STATS_$dev 2>/dev/null
  iptables-save -t mangle | grep NETWORK_STATS_$dev | grep "\-A"  | while read rule
  do
    rule=$(echo $rule | sed 's/\-A/\-D/')
    sudo iptables -t mangle $rule
  done
  sudo iptables -t mangle -X NETWORK_STATS_$dev 2>/dev/null
  # remove apache config for this eth
  rm -f /etc/apache2/conf.d/vhost$dev.conf
}

action=$1
dev=$2
tableNo=${dev:3}
tableName="Table_$dev"

if [ $action == 'add' ]
then
  plug_nic
else
  unplug_nic
fi
