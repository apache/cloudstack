#!/usr/bin/env bash


plug_nic() {
  sudo iptables -t mangle -A PREROUTING -i $dev -m state --state NEW -j MARK --set-mark $tableNo 2>/dev/null
  sudo iptables -t mangle -A PREROUTING -i $dev -m state --state NEW -j CONNMARK --save-mark 2>/dev/null

  sudo echo "$tableNo $tableName" >> /etc/iproute2/rt_tables 2>/dev/null
  sudo ip rule add fwmark $tableNo table $tableName 2>/dev/null
  sudo ip route flush table $tableName
  sudo ip route flush cache
}


unplug_nic() {
  sudo iptables -t mangle -D PREROUTING -i $dev -m state --state NEW -j MARK --set-mark $tableNo 2>/dev/null
  sudo iptables -t mangle -D PREROUTING -i $dev -m state --state NEW -j CONNMARK --save-mark 2>/dev/null

  sudo sed -i '/"$tableNo $tableName"/d' /etc/iproute2/rt_tables 2>/dev/null
  sudo ip rule delete fwmark $tableNo table $tableName 2>/dev/null
  sudo ip route flush  table $tableName
  sudo ip route flush cache
}

action=$1
dev=$2
tableNo=$(echo $dev | awk -F'eth' '{print $2}')
tableName="Table_$dev"

if [ $action == 'add' ]
then
  plug_nic
else
  unplug_nic
fi
