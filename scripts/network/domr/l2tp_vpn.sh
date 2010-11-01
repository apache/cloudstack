#!/bin/bash
#
# @VERSION@

cert="/root/.ssh/id_rsa.cloud"
domr=$1
shift
ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domr "/opt/cloud/bin/vpn_l2tp.sh $*" >/dev/null

exit $?
