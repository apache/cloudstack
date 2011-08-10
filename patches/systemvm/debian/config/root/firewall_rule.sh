#!/usr/bin/env bash
 
# $Id: firewall.sh 9947 2010-06-25 19:34:24Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/patches/xenserver/root/loadbalancer.sh $
#
#
# @VERSION@
echo $* >> /tmp/jana.log
usage() {
  printf "Usage: %s: -F  -a <added public ip address protocol:startport:endport:sourcecidr>  \n" $(basename $0) >&2
# scourcecidrs format:  n1-n2-n3-n4
}

# set -x

#FIXME: eating up the error code during execution of iptables
fw_remove_backup() {
  for vif in $VIF_LIST; do 
    sudo iptables -F back_firewall_rules_$vif 2> /dev/null
    sudo iptables -D INPUT -i $vif  -j back_firewall_rules_$vif 2> /dev/null
    sudo iptables -X back_firewall_rules_$vif 2> /dev/null
  done
}
fw_restore() {
  for vif in $VIF_LIST; do 
    sudo iptables -F firewall_rules_$vif 2> /dev/null
    sudo iptables -D INPUT -i $vif   -j firewall_rules_$vif 2> /dev/null
    sudo iptables -X firewall_rules_$vif 2> /dev/null
    sudo iptables -E back_firewall_rules_$vif firewall_rules_$vif 2> /dev/null
  done
}
# firewall entry to ensure that haproxy can receive on specified port
fw_entry() {
  local added=$1
  
  if [ "$added" == "none" ]
  then
  	added=""
  fi
  
  
  local a=$(echo $added | cut -d, -f1- --output-delimiter=" ")

# back up the iptable rules by renaming before creating new. 
  for vif in $VIF_LIST; do 
    sudo iptables -E firewall_rules_$vif back_firewall_rules_$vif 2> /dev/null
    sudo iptables -N firewall_rules_$vif 2> /dev/null
    sudo iptables -A INPUT -i $vif -j firewall_rules_$vif
  done

  for i in $a
  do
    local prot=$(echo $i | cut -d: -f1)
    local sport=$(echo $i | cut -d: -f2)    
    local eport=$(echo $i | cut -d: -f3)    
    local scidrs=$(echo $i | cut -d: -f4 | sed 's/-/,/g')
 
    
    for vif in $VIF_LIST; do 
      if [ "$prot" == "icmp" ]
      then
# TODO  icmp code need to be implemented
# sport is icmpType , dport is icmpcode 
	 if [ "$sport" == "-1" ]
         then
             sudo iptables -A firewall_rules_$vif -s $scidrs -p $prot  -j ACCEPT
         else
             sudo iptables -A firewall_rules_$vif -s $scidrs -p $prot --icmp-type $sport  -j ACCEPT
         fi
      else
         sudo iptables -A firewall_rules_$vif -s $scidrs -p $prot --dport $sport:$eport -j ACCEPT
      fi
      
      if [ $? -gt 0 ]
      then
        return 1
      fi
    done      
  done
 
  return 0
}

get_vif_list() {
  local vif_list=""
  for i in /sys/class/net/eth*; do 
    vif=$(basename $i);
    if [ "$vif" != "eth0" ] && [ "$vif" != "eth1" ]
    then
      vif_list="$vif_list $vif";
    fi
  done
  if [ "$vif_list" == "" ]
  then
      vif_list="eth0"
  fi
  
  logger -t cloud "FirewallRule public interfaces = $vif_list"
  echo $vif_list
}

shift 
while getopts 'a:' OPTION
do
  case $OPTION in
  a)	aflag=1
		rules="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

VIF_LIST=$(get_vif_list)

if [ "$rules" == "" ]
then
  rules="none"
fi

# iptables entry to ensure that haproxy receives traffic
fw_entry $rules 
  	
if [ $? -gt 0 ]
then
  logger -t cloud "Reverting firewall config"
  # Revert iptables rules on DomR
  fw_restore

  exit 1
else
  # Remove backedup iptable rules
  fw_remove_backup
fi
 
exit 0
  	

