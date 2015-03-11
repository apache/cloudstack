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
import errno
from socket import error as socket_error

from xmlrpclib import ServerProxy, Error

def spCon(proto, auth, host, port):
    print "trying %s on %s@%s:%s" % (proto, auth, host, port)
    try:
        x = ServerProxy("%s://%s@%s:%s" % (proto, auth, host, port))
        x.echo(proto)
        return x
    except Error, v:
        return
    except socket_error, serr:
        return

def getCon(auth, host, port):
    try:
        server = spCon("http", auth, host, port)
        if server:
            return server
        else:
            server = spCon("https", auth, host, port)
    except Error, v:
        print "ERROR", v
    return server

def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])

# hmm master actions don't apply to a slave
master = "192.168.1.161"
port = 8899
passw = 'test123'
user = 'oracle'
auth = "%s:%s" % (user, passw)
server = getCon(auth, "localhost", port)
mserver = getCon(auth, master, port)
try:
    mserver.echo("test")
except AttributeError, v:
    print "no mserver, becoming mserver"
    mserver = server

poolNode = True
interface = "c0a80100"
role = 'xen,utility'
hostname = gethostname()
ip = get_ip_address(interface)
nodes = []

try:
    # pooling related same as primary storage!
    poolalias = "Pool 0"
    clusterid = "ba9aaf00ae5e2d72"
    mgr = "d1a749d4295041fb99854f52ea4dea97"
    poolmvip = master

    # primary
    primuuid = "7718562d872f47a7b4548f9cac4ffa3a"
    ssuuid = "7718562d-872f-47a7-b454-8f9cac4ffa3a"
    fshost = "cs-mgmt"
    fstarget = "/volumes/cs-data/primary/ovm"
    fstype = "nfs"
    fsname = "Primary storage"
    fsmntpoint = "%s:%s" % (fshost, fstarget)
    fsmntpoint2 = "%s:%s" % (fshost, "/volumes/cs-data/secondary")
    fsmntpoint = "%s/VirtualMachines" % (fsmntpoint2)
    fsmnt = "/nfsmnt/%s" % (ssuuid)
    fsplugin = "oracle.generic.NFSPlugin.GenericNFSPlugin"
    repo = "/OVS/Repositories/%s" % (primuuid)

    # set the basics we require to "operate"
    print server.take_ownership(mgr, '')
    print server.update_server_roles(role,)

    # setup the repository
    print server.mount_repository_fs(fsmntpoint2, repo)
    try:
        print server.add_repository(fsmntpoint2, repo)
    except Error, v:
        print "will create the repo, as it's not there", v
        print server.create_repository(fsmntpoint2, repo, primuuid, "A repository")

    # if we're pooling pool...
    if (poolNode == True):
        poolCount = 0
        pooled = False

        # check pooling
        poolDom = parseString(mserver.discover_server_pool())
        for node in poolDom.getElementsByTagName('Server_Pool'):
            id = node.getElementsByTagName('Unique_Id')[0].firstChild.nodeValue
            alias = node.getElementsByTagName('Pool_Alias')[0].firstChild.nodeValue
            mvip = node.getElementsByTagName('Master_Virtual_Ip')[0].firstChild.nodeValue
            print "pool: %s, %s, %s" % (id, mvip, alias)
            members = node.getElementsByTagName('Member')
            for member in members:
                poolCount = poolCount + 1
                mip = member.getElementsByTagName('Registered_IP')[0].firstChild.nodeValue
                if (mip == ip):
                    pooled = True
                else:
                    nodes.append(mip)
                print "member: %s" % (mip)

        # if (pooled == False):
        try:
            if (poolCount == 0):
                print "master"
                # check if a pool exists already if not create
                # pool if so add us to the pool
                print server.configure_virtual_ip(master, ip)
                print server.create_pool_filesystem(
                    fstype,
                    fsmntpoint,
                    clusterid,
                    primuuid,
                    ssuuid,
                    mgr,
                    primuuid
                )
                print server.create_server_pool(poolalias,
                    primuuid,
                    poolmvip,
                    poolCount,
                    hostname,
                    ip,
                    role
                )
            else:
                try:
                    print "slave"
                    print server.join_server_pool(poolalias,
                        primuuid,
                        poolmvip,
                        poolCount,
                        hostname,
                        ip,
                        role
                    )
                except Error, v:
                    print "host already part of pool?: %s" % (v)

            nodes.append(ip)
            for node in nodes:
                # con = getCon(auth, node, port)
                # print con.set_pool_member_ip_list(nodes);
                print mserver.dispatch("http://%s@%s:%s/api/3" % (auth, node, port), "set_pool_member_ip_list", nodes)
            # print server.configure_virtual_ip(master, ip)
        except Error, e:
            print "something went wrong: %s" % (e)

    # sys.exit()
    # mount the primary fs
    print server.storage_plugin_mount(
        fsplugin,
        {
            'uuid': primuuid,
            'storage_desc': fsname,
            'access_host': fshost,
            'storage_type': 'FileSys',
            'name':primuuid
       },
        {
            'status': '',
            'uuid': ssuuid,
            'ss_uuid': primuuid,
            'size': 0,
            'state': 1,
            'access_grp_names': [],
            'access_path': fsmntpoint,
            'name': fsname
        },
        fsmnt,
        '',
        True,
        []
    )

except Error, v:
    print "ERROR", v
