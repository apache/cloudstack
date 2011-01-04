#!/bin/bash

#set -x
 
usage() {
  printf "Usage: %s \n" $(basename $0) >&2

}
moveConfigToBond() {
  bond=$1
  bondMaster=$(xe bond-param-get param-name=master uuid=$bond)
  bondMasterIp=$(xe pif-param-get param-name=IP uuid=$bondMaster)
  if [ -z "$bondMasterIp" ]; then
    bondSlaves=$(xe bond-param-get param-name=slaves uuid=$bond| tr ';' ' ')
    for bondSlave in $bondSlaves; do
      bondSlaveIp=$(xe pif-param-get param-name=IP uuid=$bondSlave)
      if [ -n "$bondSlaveIp" ]; then
        mode=$(xe pif-param-get param-name=IP-configuration-mode uuid=$bondSlave)
        netmask=$(xe pif-param-get param-name=netmask uuid=$bondSlave)
        gateway=$(xe pif-param-get param-name=gateway uuid=$bondSlave)       
        DNS=$(xe pif-param-get param-name=DNS uuid=$bondSlave)
        management=$(xe pif-param-get param-name=management uuid=$bondSlave)
        xe pif-reconfigure-ip DNS=$DNS gateway=$gateway IP=$bondSlaveIp mode=$mode netmask=$netmask uuid=$bondMaster
        if [ $? -ne 0 ]; then
          echo "  --Failed to program pif $bondMaster , please run xe pif-reconfigure-ip DNS=$DNS gateway=$gateway IP=$bondSlaveIp mode=$mode netmask=$netmask uuid=$bondMaster manually"
          exit 1
        fi
        echo "  --program pif $bondMasterace DNS=$DNS gateway=$gateway IP=$bondSlaveIp mode=$mode netmask=$netmask"
        if [ "$management" = "true" ]; then
          xe host-management-reconfigure pif-uuid=$bondMaster 
          if [ $? -ne 0 ]; then
            echo "  --Failed to move management interface from $bondSlave to $bondMaster, please run xe host-management-reconfigure pif-uuid=$bondMaster manually"
            exit 1
          fi
          echo "  --move management interface from $bondSlave to $bondMaster"
        fi
        xe pif-reconfigure-ip mode=None uuid=$bondSlave
        break
      fi
      xe pif-plug uuid=$bondMaster
    done
  fi
} 

poolUuid=$(xe pool-list | grep ^uuid | awk '{print $NF}')
hostMaster=$(xe pool-param-get uuid=$poolUuid param-name=master)

echo "#Begin check"
echo "##check master $hostMaster"
masterPifs=$(xe pif-list host-uuid=$hostMaster| grep ^uuid | awk '{print $NF}')
for pif in $masterPifs; do
  bond=$(xe pif-param-get param-name=bond-master-of uuid=$pif)
  if [ -n "$bond" ]; then
    moveConfigToBond $bond
  fi
done
echo "##done for master $hostMaster"

hostSlaves=$(xe host-list | grep ^uuid | awk '{print $NF}')

for hostSlave in $hostSlaves; do
  if [ "$hostSlave" != "$hostMaster" ]; then
    echo "##check slave $hostSlave"
    slavePifs=$(xe pif-list host-uuid=$hostSlave| grep ^uuid | awk '{print $NF}')
    for slavePif in $slavePifs; do
      bond=$(xe pif-param-get param-name=bond-master-of uuid=$slavePif)
      if [ -n "$bond" ]; then
        moveConfigToBond $bond
      fi
    done
    for masterPif in $masterPifs; do
      network=$(xe pif-param-get param-name=network-uuid uuid=$masterPif)
      management=$(xe pif-param-get param-name=management uuid=$masterPif)
      slavePif=$(xe pif-list host-uuid=$hostSlave network-uuid=$network | grep ^uuid | awk '{print $NF}')
      if [ -z "$slavePif" ]; then
        echo "  --ERROR:There is no NIC $device on host $hostSlave on network $network, please check"
        exit 1
      fi
    done
    echo "##done for slave $hostSlave"
  fi
done
echo "#check is successful, add these hosts to cloud stack"

