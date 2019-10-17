#!/bin/sh
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

errExit() {
    echo $@
    exit 1
}

stopHeartbeat() {
    pidFile="/var/run/ovs-agent/heartbeat.pid"
    if [ -f $pidFile ]; then
        pid=`cat $pidFile`
        ps -p $pid &>/dev/null
        if [ $? -eq 0 ]; then
            kill $pid &>/dev/null
        fi
    fi
}

openPortOnIptables() {
	port="$1"
	protocol="$2"
    chkconfig --list iptables | grep "on"
	if [ $? -eq 0 ]; then
	    iptables-save | grep "A INPUT -p $protocol -m $protocol --dport $port -j ACCEPT" >/dev/null
	    if [ $? -ne 0 ]; then
	        iptables -I INPUT 1 -p $protocol --dport $port -j ACCEPT
	        if [ $? -ne 0 ]; then
	            exit_with_error "iptables -I INPUT 1 -p $protocol --dport $port -j ACCEPT failed"
	        fi
	        echo "iptables:Open $protocol port $port for DHCP"
	    fi
	fi
}

applyPatch() {
    patchFile="$1"
    level="$2"

    [ ! -f $patchFile ] && errExit "Can not find $patchFile"

    if [ $? -ne 0 ]; then
        pushd /opt/ovs-agent-latest &>/dev/null
        test=`patch -p$level --dry-run -N < $patchFile`
        if [ $? -ne 0 ]; then
            tmp=`mktemp`
            echo $test > $tmp
            grep "Reversed (or previously applied) patch detected" $tmp &>/dev/null
            if [ $? -eq 0 ]; then
                # The file has been patched
                rm $tmp -f
                popd &>/dev/null
                return
            else
                rm $tmp -f
                popd &>/dev/null
                errExit "Can not apply $patchFile beacuse $test"
            fi
        fi
        patch -p$level < $patchFile
        [ $? -ne 0 ] && errExit "Patch to $target failed"
        popd &>/dev/null
    fi
}

postSetup() {
    openPortOnIptables 7777 tcp # for OCFS2, maybe tcp only
    openPortOnIptables 7777 udp
    openPortOnIptables 3260 tcp # for ISCSI, maybe tcp only
    openPortOnIptables 3260 udp
    applyPatch "/opt/ovs-agent-latest/OvmPatch.patch" 2
    applyPatch "/opt/ovs-agent-latest/OvmDontTouchOCFS2ClusterWhenAgentStart.patch" 1
    applyPatch "/opt/ovs-agent-latest/Fixget_storage_reposExceptionDueToWrongReturnValueCheck.patch" 1

    stopHeartbeat

    /etc/init.d/ovs-agent restart --disable-nowayout
    [ $? -ne 0 ] && errExit "Restart ovs agent failed"
    exit 0
}

preSetup() {
    agentConfig="/etc/ovs-agent/agent.ini"
    agentInitScript="/etc/init.d/ovs-agent"

    [ ! -f $agentConfig ] && errExit "Can not find $agentConfig"
    [ ! -f $agentInitScript ] && errExit "Can not find $agentInitScript"

    version=`grep "version="  $agentInitScript | cut -d "=" -f 2`
    [ x"$version" != x"2.3" ] && errExit "The OVS agent version is $version, we only support 2.3 now"

    # disable SSL
    sed -i 's/ssl=enable/ssl=disable/g' $agentConfig
    [ $? -ne 0 ] && errExit "configure ovs agent to non ssl failed"

    if [ ! -L /opt/ovs-agent-latest ]; then
        eval $agentInitScript status | grep 'down' && $agentInitScript start
        [ $? -ne 0 ] && errExit "Start ovs agent failed"
        [ ! -L /opt/ovs-agent-latest ] && errExit "No link at /opt/ovs-agent-latest"
    fi
    exit 0
}

[ $# -ne 1 ] && errExit "Usage: configureOvm.sh command"

case "$1" in
    preSetup)
        preSetup
        ;;
    postSetup)
        postSetup
        ;;
    *)
        errExit "Valid commands: preSetup postSetup"
esac

