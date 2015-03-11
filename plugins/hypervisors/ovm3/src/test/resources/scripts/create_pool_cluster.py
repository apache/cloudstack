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
import os, sys, subprocess
from xml.dom.minidom import parseString

from xmlrpclib import ServerProxy, Error

server = ServerProxy("http://localhost:8899")

pooledFs = 1
normalRepo = 0

try:
  if normalRepo:
    print "normal repo"
    # this litterally throws EVERYTHING away on the repo
    repoDom = parseString(server.discover_repository_db())
    for node in repoDom.getElementsByTagName('Repository'):
        repoUuid = node.attributes['Uuid']
        remoteMount = node.getElementsByTagName('Fs_location')[0].firstChild.nodeValue
        localMount = node.getElementsByTagName('Mount_point')[0].firstChild.nodeValue

        # there is a "strong" relation between repo's and VMs
        # onfortunately there is no reference in the vm.cfg
        # or any known info in the configuration of the VM
        # in which repo it lives....
        for dirname, dirnames, filenames in os.walk('%s/VirtualMachines/' % localMount):
            for vm in dirnames:
                print "Destroying vm: %s on repo %s" % (vm, repoUuid.value)
                try:
                    mVm = server.list_vm(repoUuid.value, vm)
                    if mVm != None:
                        print server.stop_vm(repoUuid.value, vm)
                        print server.delete_vm(repoUuid.value, vm)
                    else:
                        print "%s already not in repo %s" % (repoUuid.value, vm)
                except Error, v:
                    print "Unable to destroy: %s" % (v)
                    continue

        # VMs = server.list_vms()
        # for vm in VMs:
        #    if vm['domid'] != '0':
        #        print vm
        #        print server.delete_vm(repoUuid.value, vm['uuid'])

        rc = server.delete_repository(repoUuid.value, True)
        # Set to false if you want to keep data:      ^^^^
        print "Repository: %s" % repoUuid.value
        if (rc == None):
            print "Ok repo: %s destroyed!" % repoUuid.value
            # now unmount the FS
            # print server.unmount_repository_fs(localMount)
        else:
            print "Failed repo: %s not destroyed!" % repoUuid.value

    # for now only treat NFS stuff as we're testing with that..
    nfsHost = 'cs-mgmt'
    nfsDom = server.storage_plugin_listMountPoints(
        'oracle.generic.NFSPlugin.GenericNFSPlugin',
            { 'status': '',
                'admin_user': '',
                'admin_host': '',
                'uuid': '',
                'total_sz': 0,
                'admin_passwd': '',
                'free_sz': 0,
                'name': '',
                'access_host': nfsHost,
                'storage_type': 'FileSys',
                'alloc_sz': 0,
                'access_grps': [],
                'used_sz': 0,
                'storage_desc': ''
            })
    for node in nfsDom:
        props = {'status': node['status'],
            'uuid': '',
            'access_host': nfsHost,
            'storage_type': 'FileSys',
            'name': '' }
        extprops = {'status': node['status'],
            'uuid': node['fs_uuid'],
            'ss_uuid': '',
            'size': 0,
            'free_sz': '',
            'state': 1,
            'access_grp_names': [],
            'access_path': nfsHost + ':' + '/volumes/cs-data/secondary',
            'name': ''}
        # rc = server.storage_plugin_unmount('oracle.generic.NFSPlugin.GenericNFSPlugin', props, extprops, nfsMnt, True)
        # print rc

    nfsDom = parseString(server.discover_mounted_file_systems('nfs'))
    for node in nfsDom.getElementsByTagName('Mount'):
        nfsMnt = node.attributes['Dir'].value
        print 'Mountpoint: %s' % (nfsMnt)
        fsStamp = '%s/.generic_fs_stamp' % nfsMnt
        # remove this so we don't cock up next run
        if os.path.isfile(fsStamp):
            print "Stamp found: %s" % fsStamp
            os.unlink(fsStamp)

        rc = server.storage_plugin_unmount('oracle.generic.NFSPlugin.GenericNFSPlugin', props, extprops, nfsMnt, True)
        print rc


  if pooledFs:
    print "pooling"
    # pool stuff
    poolalias = "ItsMyPool"
    poolmvip = "192.168.1.161"
    poolfirsthost = {
        'ip': "192.168.1.64",
        'hn': "ovm-1",
        'id': 0,
        'role': 'utility,xen'
    }
    fstype = "nfs"
    fstarget = "cs-mgmt:/volumes/cs-data/primary"
    poolid = "0004fb0000020000ba9aaf00ae5e2d73"
    clusterid = "ba9aaf00ae5e2d72"
    poolfsuuid = "0004fb0000050000e70fbddeb802208f"
    poolfsnfsbaseuuid = "b8ca41cb-3469-4f74-a086-dddffe37dc2d"
    manageruuid = "0004fb00000100000af70d20dcce7d65"
    pooluuid = "0004fb0000020000ba9aaf00ae5e2d73"
    blocksize = ""
    clustersize = ""
    journalesize = ""

    # o2cb is the problem.... /etc/init.d/o2cb
    #   sets it's config in /etc/sysconfig/o2cb (can be removed)
    #   dmsetup requires the stopping of o2cb first,
    #   then the removal of the config, after which dmsetup
    #   can remove the device from /dev/mapper/
    # eventually cluster cleanup can be done by removing
    #   stuff from /etc/ovs-agent/db
    #   also clean /etc/ocfs2/cluster.conf
    print server.create_pool_filesystem(
        fstype,
        fstarget,
        clusterid,
        poolfsuuid,
        poolfsnfsbaseuuid,
        manageruuid,
        pooluuid
    )

    # poolDom = server.discover_server_pool()
    # print poolDom
    # poolDom = parseString(server.discover_server_pool())
    # if poolDom.getElementsByTagName('Server_Pool'):
    # get unique id
    cluster = server.is_cluster_online()
    if cluster == True:
        print "clean up pool"
        # print server.destroy_cluster(poolfsuuid)
        # deconfigure cluster
        # print server.destroy_server_pool(poolid)

    if cluster == False:
        print "create_server_pool"
        # first take ownership. without an owner nothing happens
        print server.take_ownership(manageruuid, "")
        # we need to add the first host first to the pool....
        poolDom = server.discover_server_pool()
        print poolDom
        poolDom = parseString(server.discover_server_pool())
        if poolDom.getElementsByTagName('Server_Pool'):
            print server.destroy_server_pool(pooluuid)

        print server.create_pool_filesystem(
            fstype,
            fstarget,
            clusterid,
            poolfsuuid,
            poolfsnfsbaseuuid,
            manageruuid,
            pooluuid
        )
        print server.create_server_pool(poolalias,
            pooluuid,
            poolmvip,
            poolfirsthost['id'],
            poolfirsthost['hn'],
            poolfirsthost['ip'],
            poolfirsthost['role'])

        print "configure_virtual_ip"
        server.configure_virtual_ip(poolmvip, poolfirsthost['ip'])
        server.set_pool_member_ip_list(['192.168.1.64', '192.168.1.65'],)
        print "configure for cluster"
        server.configure_server_for_cluster(
            {
                'O2CB_HEARTBEAT_THRESHOLD': '61',
                'O2CB_RECONNECT_DELAY_MS': '2000',
                'O2CB_KEEPALIVE_DELAY_MS': '2000',
                'O2CB_BOOTCLUSTER': clusterid,
                'O2CB_IDLE_TIMEOUT_MS': '60000',
                'O2CB_ENABLED': 'true',
                'O2CB_STACK': 'o2cb'
            },
            {
                'node': [
                    {
                        'ip_port': 7777,
                        'cluster': clusterid,
                        'ip_address': poolfirsthost['ip'],
                        'name': poolfirsthost['hn'],
                        'number': poolfirsthost['id']
                    }
                ],
                'heartbeat': [
                    {
                        'cluster': clusterid,
                        # uppercase poolfsuuid
                        'region': '0004FB0000050000E70FBDDEB802208F'
                    }
                ],
                'cluster': [
                    {
                        'heartbeat_mode': 'global',
                        'node_count': 1,
                        'name': clusterid
                    }
                ]
            },
            'nfs',
            'cs-mgmt:/volumes/cs-data/primary',
            poolfsuuid,
            poolfsnfsbaseuuid
        )
        print "create cluster"
        server.create_cluster(poolfsuuid,)

    poolDom = parseString(server.discover_server_pool())
    for node in poolDom.getElementsByTagName('Server_Pool'):
        id = node.getElementsByTagName('Unique_Id')[0].firstChild.nodeValue
        alias = node.getElementsByTagName('Pool_Alias')[0].firstChild.nodeValue
        mvip = node.getElementsByTagName('Master_Virtual_Ip')[0].firstChild.nodeValue
        print "pool: %s, %s, %s" % (id, mvip, alias)
        members = node.getElementsByTagName('Member')
        for member in members:
            mip = member.getElementsByTagName('Registered_IP')[0].firstChild.nodeValue
            print "member: %s" % (mip)

    print server.is_cluster_online()
    print server.discover_cluster()
    print server.discover_pool_filesystem()
    print server.discover_server_pool()
    # server.destroy_server_pool(pooluuid)

except Error, v:
    print "ERROR", v
