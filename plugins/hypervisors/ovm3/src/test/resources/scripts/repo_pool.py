#!/usr/bin/python
#
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
#
import os, sys, subprocess, socket, fcntl, struct
from socket import gethostname
from xml.dom.minidom import parseString

from xmlrpclib import ServerProxy, Error

def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])

def is_it_up(host, port):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1)
        s.connect((host, port))
        s.close()
    except:
        print "host: %s:%s DOWN" % (host, port)
        return False

    print "host: %s:%s UP" % (host, port)
    return True

# hmm master actions don't apply to a slave
master = "192.168.1.161"
port = 8899
user = "oracle"
password = "*******"
auth = "%s:%s" % (user, password)
server = ServerProxy("http://%s:%s" % ("localhost", port))
mserver = ServerProxy("http://%s@%s:%s" % (auth, master, port))
poolNode = True
interface = "c0a80100"
role = 'xen,utility'
hostname = gethostname()
ip = get_ip_address(interface)
poolMembers = []
xserver = server

print "setting up password"
server.update_agent_password(user, password)

if (is_it_up(master, port)):
    print "master seems to be up, slaving"
    xserver = mserver
else:
    print "no master yet, will become master"

# other mechanism must be used to make interfaces equal...
try:
    # pooling related same as primary storage!
    poolalias = "Pool 0"
    poolid = "0004fb0000020000ba9aaf00ae5e2d73"
    poolfsnfsbaseuuid = "7718562d-872f-47a7-b454-8f9cac4ffa3a"
    pooluuid = poolid
    poolfsuuid = poolid
    clusterid = "ba9aaf00ae5e2d72"
    mgr = "d1a749d4295041fb99854f52ea4dea97"
    poolmvip = master

    poolfsnfsbaseuuid = "6824e646-5908-48c9-ba44-bb1a8a778084"
    repoid = "6824e646590848c9ba44bb1a8a778084"
    poolid = repoid
    repo = "/OVS/Repositories/%s" % (repoid)
    repomount = "cs-mgmt:/volumes/cs-data/secondary"

    # primary
    primuuid = "7718562d872f47a7b4548f9cac4ffa3a"
    ssuuid = "7718562d-872f-47a7-b454-8f9cac4ffa3a"
    fshost = "cs-mgmt"
    fstarget = "/volumes/cs-data/primary"
    fstype = "nfs"
    fsname = "Primary storage"
    fsmntpoint = "%s:%s" % (fshost, fstarget)
    fsmnt = "/nfsmnt/%s" % (ssuuid)
    fsplugin = "oracle.generic.NFSPlugin.GenericNFSPlugin"

    # set the basics we require to "operate"
    print server.take_ownership(mgr, '')
    print server.update_server_roles(role,)

    # if we're pooling pool...
    if (poolNode == True):
        poolCount = 0
        pooled = False

        # check pooling
        try:
            poolDom = parseString(xserver.discover_server_pool())
            print xserver.discover_server_pool()
            for node in poolDom.getElementsByTagName('Server_Pool'):
                id = node.getElementsByTagName('Unique_Id')[0].firstChild.nodeValue
                alias = node.getElementsByTagName('Pool_Alias')[0].firstChild.nodeValue
                mvip = node.getElementsByTagName('Master_Virtual_Ip')[0].firstChild.nodeValue
                print "pool: %s, %s, %s" % (id, mvip, alias)
                members = node.getElementsByTagName('Member')
                for member in members:
                    poolCount = poolCount + 1
                    mip = member.getElementsByTagName('Registered_IP')[0].firstChild.nodeValue
                    print "member: %s" % (mip)
                    if mip == ip:
                        pooled = True
                    else:
                        poolMembers.append(mip)

        except Error, v:
            print "no master will become master, %s" % v

        if (pooled == False):
            # setup the repository
            print "setup repo"
            print server.mount_repository_fs(repomount, repo)
            try:
                print "adding repo"
                print server.add_repository(repomount, repo)
            except Error, v:
                print "will create the repo, as it's not there", v
                print server.create_repository(repomount, repo, repoid, "repo")

            print "not pooled!"
            if (poolCount == 0):
                print "no pool yet, create it"
                # check if a pool exists already if not create
                # pool if so add us to the pool
                print "create pool fs"
                print server.create_pool_filesystem(
                    fstype,
                    "%s/VirtualMachines/" % repomount,
                    clusterid,
                    poolfsuuid,
                    poolfsnfsbaseuuid,
                    mgr,
                    pooluuid
                )
                print "create pool"
                print server.create_server_pool(poolalias,
                    pooluuid,
                    poolmvip,
                    poolCount,
                    hostname,
                    ip,
                    role
                )
            else:
                print "join the pool"
                print server.join_server_pool(poolalias,
                    pooluuid,
                    poolmvip,
                    poolCount,
                    hostname,
                    ip,
                    role
                )

        # add member to ip list ?
        poolMembers.append(ip)
        print "mambers for pool: %s" % poolMembers
        print xserver.set_pool_member_ip_list(poolMembers)

    print server.discover_server_pool()

except Error, v:
    print "ERROR", v
