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

if [ -z $1 ]
then
    echo "Fail to find VPN peer address!"
    exit 1
fi

ipsec auto --status | grep vpn-$1 > /tmp/vpn-$1.status

cat /tmp/vpn-$1.status | grep "ISAKMP SA established" > /dev/null
isakmpok=$?
if [ $isakmpok -ne 0 ]
then
    echo -n "ISAKMP SA NOT found but checking IPsec;"
else
    echo -n "ISAKMP SA found;"
fi

cat /tmp/vpn-$1.status | grep "IPsec SA established" > /dev/null
ipsecok=$?
if [ $ipsecok -ne 0 ]
then
    echo -n "IPsec SA not found;"
    echo "Site-to-site VPN have not connected"
    exit 11
fi
echo -n "IPsec SA found;"
echo "Site-to-site VPN have connected"
exit 0
