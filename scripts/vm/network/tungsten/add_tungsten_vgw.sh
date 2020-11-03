CONTAINER=$(docker ps | grep contrail-vrouter-agent | awk '{print $11}')
docker exec $CONTAINER python /opt/contrail/utils/provision_vgw_interface.py --oper create --interface $1 --subnets $2 --routes $3 --vrf $4
docker exec $CONTAINER ip netns exec $5 ip route replace default via $6