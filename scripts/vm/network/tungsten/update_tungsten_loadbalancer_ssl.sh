while [ ! -f /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf ]; do sleep 1; done
if grep -qw "bind $5:$6 $" /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf; then
rm -rf /var/lib/contrail/loadbalancer/haproxy/$1/$2.pem
cat >> /var/lib/contrail/loadbalancer/haproxy/$1/$2.pem << EOF
$3$4
EOF
sed -i "/bind $5:$6 $/c\	bind $5:$6 ssl crt /var/lib/contrail/loadbalancer/haproxy/$1/$2.pem" /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf
container=$(docker ps | grep contrail-vrouter-agent | awk '{print $1}')
netns=$(ls /var/run/netns | grep $1)
docker exec $container ip netns exec $netns haproxy -D -f /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf \
-p /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.pid -sf $(cat /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.pid) &>/dev/null
fi
