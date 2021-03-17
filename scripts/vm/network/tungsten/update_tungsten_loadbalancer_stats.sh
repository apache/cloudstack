while [ ! -f /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf ]; do sleep 1; done
if ! grep -q "listen stats" /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf; then
cat << EOF >> /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf
listen stats :$2
	mode http
	stats enable
	stats realm Haproxy\ Statistics
	stats uri $3
	stats auth $4
EOF
container=$(docker ps | grep contrail-vrouter-agent | awk '{print $1}')
netns=$(ls /var/run/netns | grep $1)
docker exec $container ip netns exec $netns haproxy -D -f /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.conf \
-p /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.pid -sf $(cat /var/lib/contrail/loadbalancer/haproxy/$1/haproxy.pid) &>/dev/null
fi
