netns=$(ls /var/run/netns)
container=$(docker ps | grep contrail-vrouter-agent | awk '{print $1}')
docker exec $container python /opt/contrail/utils/provision_vgw_interface.py --oper create --interface $1 --subnets $2 --routes $3 --vrf $4
docker exec $container ip netns exec $netns ip route replace default via $5