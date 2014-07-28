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

vpn_service() {
	ps aux|grep ipsec | grep -v grep > /dev/null
	no_vpn=$?
	if [ $no_vpn -eq 1 ]
	then
		return 0
	fi
	r=0
	case "$1" in
		stop)
			service ipsec stop && \
			service xl2tpd stop
			r=$?
			;;
		restart)
			service ipsec restart && \
			service xl2tpd restart
			r=$?
			;;
	esac
	return $r
}

ret=0
case "$1" in
    start)
	vpn_service restart && \
        service cloud-passwd-srvr start && \
        service dnsmasq start
	ret=$?
        ;;
    stop)
	vpn_service stop && \
        service cloud-passwd-srvr stop && \
        service dnsmasq stop
	ret=$?
        ;;
    restart)
	vpn_service restart && \
        service cloud-passwd-srvr restart && \
        service dnsmasq restart
	ret=$?
        ;;
    *)
        echo "Usage: services {start|stop|restart}"
        exit 1
	;;
esac

exit $ret
