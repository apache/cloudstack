# -*- encoding: utf-8 -*-
#
# Copyright (c) 2011 Citrix.  All rights reserved.
#

""" Base class for all Cloudstack resources - Server, Volume, Snapshot etc
"""

from utils import is_server_ssh_ready, random_gen
from cloudstackAPI import *

class Server:
    """Manage server lifecycle
    """
    def __init__(self, items, services):
        self.__dict__.update(items)
        self.username = services["username"]
        self.password = services["password"]
        self.ssh_port = services["ssh_port"]
        self.ssh_client = None
        #extract out the ipaddress
        self.ipaddress = self.nic[0].ipaddress
        
    @classmethod
    def create(cls, apiclient, services):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.serviceofferingid = services["serviceoffering"]
        cmd.templateid = services["template"]
        cmd.zoneid = services["zoneid"]
        if "diskoffering" in services:
            cmd.diskofferingid = services["diskoffering"]
        return Server(apiclient.deployVirtualMachine(cmd).__dict__, services)

    def get_ssh_client(self):
            self.ssh_client = self.ssh_client or is_server_ssh_ready(
                                                    self.ipaddress,
                                                    self.ssh_port,
                                                    self.username,
                                                    self.password
                                                )
            return self.ssh_client

    def delete(self, apiclient):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.id
        apiclient.destroyVirtualMachine(cmd)


class Volume:
    """Manage Volume Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)
        
    @classmethod
    def create(cls, apiclient, services):
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]
        cmd.diskofferingid = services["volumeoffering"]
        cmd.zoneid = services["zoneid"]
        return Volume(apiclient.createVolume(cmd).__dict__)
    

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot_id, services):
        cmd = createVolume.createVolumeCmd()    
        cmd.name = "-".join([services["diskname"], random_gen()])
        cmd.snapshotid = snapshot_id
        cmd.zoneid = services["zoneid"]
        return Volume(apiclient.createVolume(cmd).__dict__)
        
    def delete(self, apiclient):
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.id
        apiclient.deleteVolume(cmd)

class Snapshot:
    """Manage Snapshot Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, volume_id):
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = volume_id
        return Snapshot(apiclient.createSnapshot(cmd).__dict__)

    def delete(self, apiclient):
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = self.id
        apiclient.deleteSnapshot(cmd)

