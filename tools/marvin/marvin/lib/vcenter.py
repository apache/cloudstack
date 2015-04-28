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

from pyVmomi import vim, vmodl
from pyVim import connect
import atexit
import ssl
if hasattr(ssl, '_create_unverified_context'):
    ssl._create_default_https_context = ssl._create_unverified_context


class Vcenter():

    def __init__(self, host, user, pwd):
        """
        create a service_instance object
        """
        self.service_instance = connect.SmartConnect(host=host,
                                                    user=user,
                                                    pwd=pwd)
        atexit.register(connect.Disconnect, self.service_instance)

    @staticmethod
    def _parse_dvswitch(obj):
        """
        :param obj:
        :return:
        """
        parsed_dvswitch_details = {}
        parsed_dvswitch_details['name'] = obj.name
        parsed_dvswitch_details['numPorts'] = obj.summary.numPorts
        parsed_dvswitch_details['numHosts'] = obj.summary.numHosts
        if obj.summary.vm is not None:
            parsed_dvswitch_details['vm'] = obj.summary.vm
        if obj.summary.vm is not None:
            parsed_dvswitch_details['host'] = obj.summary.host
        parsed_dvswitch_details['portgroupNameList'] = obj.summary.portgroupName
        parsed_dvswitch_details['uuid'] = obj.summary.uuid
        parsed_dvswitch_details['raw'] = obj
        return parsed_dvswitch_details

    @staticmethod
    def _parse_dc(obj):
        """
        :param obj:
        :return:
        """
        parsed_dc_details = {}
        parsed_dc_details[obj.name] = {}
        clusters = obj.hostFolder.childEntity
        i = 0
        parsed_dc_details[obj.name]['clusters'] = []
        for cluster in clusters:
            parsed_dc_details[obj.name]['clusters'].append({cluster.name: []})
            hosts = cluster.host
            for host in hosts:
                parsed_dc_details[obj.name]['clusters'][i][cluster.name].append({'hostname': host.name})
            i += 1
        parsed_dc_details['raw'] = obj
        return parsed_dc_details


    @staticmethod
    def _parse_dvportgroup(obj):
        """
        :param obj:
        :return:
        """
        parsed_dvportgroup_details = {}
        parsed_dvportgroup_details['name'] = obj.name
        parsed_dvportgroup_details['numPorts'] = obj.config.numPorts
        parsed_dvportgroup_details['hostlist'] = obj.host
        parsed_dvportgroup_details['vmlist'] = obj.vm
        parsed_dvportgroup_details['tag'] = obj.tag
        parsed_dvportgroup_details['dvswitch'] = obj.config.distributedVirtualSwitch.name
        parsed_dvportgroup_details['raw'] = obj
        return parsed_dvportgroup_details

    @staticmethod
    def _parse_vm(obj):
        """
        :param obj:
        :return:
        """
        vm_details = {}
        vm_details['name'] = obj.name
        vm_details['numCpu'] = obj.summary.config.numCpu
        vm_details['tags'] = obj.tag
        vm_details['networks'] = []
        if obj.network:
            for network in obj.network:
                vm_details['networks'].append({'name': network.name})
        vm_details['raw'] = obj
        return vm_details

    def parse_details(self, obj, vimtype):
        """
        :param obj:
        :param vimtype:
        :return:
        """
        parsedObject = {}
        if vim.dvs.VmwareDistributedVirtualSwitch in vimtype:
            parsedObject['dvswitch'] = Vcenter._parse_dvswitch(obj)
        elif vim.Datacenter in vimtype:
            parsedObject['dc'] = Vcenter._parse_dc(obj)
        elif vim.dvs.DistributedVirtualPortgroup in vimtype:
            parsedObject['dvportgroup'] = Vcenter._parse_dvportgroup(obj)
        elif vim.VirtualMachine in vimtype:
            parsedObject['vm'] = Vcenter._parse_vm(obj)
        else:
            parsedObject['name'] = obj.name
        return parsedObject

    def _get_obj(self, vimtype, name=None):
        """
        Get the vsphere object associated with a given text name
        """
        obj = None
        content = self.service_instance.RetrieveContent()
        container = content.viewManager.CreateContainerView(content.rootFolder, vimtype, True)
        for c in container.view:
            if name is not None:
                if c.name == name:
                    obj = c
                    return [self.parse_details(obj, vimtype)]
            else:
                return [self.parse_details(c, vimtype) for c in container.view]

    def get_dvswitches(self, name=None):
        """
        :param name:
        :return:
        """
        dvswitches = self._get_obj([vim.dvs.VmwareDistributedVirtualSwitch], name)
        return dvswitches

    def get_datacenters(self, name=None):
        """
        :param name:
        :return:
        """
        data_centers = self._get_obj([vim.Datacenter], name)
        return data_centers


    def get_dvportgroups(self, name=None):
        """
        :param name:
        :return:
        """
        dv_portgroups = self._get_obj([vim.dvs.DistributedVirtualPortgroup], name)
        return dv_portgroups


    def get_vms(self, name=None):
        """
        :param name:
        :return:
        """
        vms = self._get_obj([vim.VirtualMachine], name)
        return vms

    def get_clusters(self, dc, clus=None):
        """
        :param dc:
        :param clus:
        :return:
        """
        pass


if __name__ == '__main__':
    vc_object = Vcenter("10.x.x.x", "username", "password")


    print '###get one dc########'
    print(vc_object.get_datacenters(name='testDC'))

    print '###get multiple dcs########'
    for i in vc_object.get_datacenters():
        print(i)

    print '###get one dv########'
    print vc_object.get_dvswitches(name='dvSwitch')

    print '###get multiple dvs########'
    for i in vc_object.get_dvswitches():
        print(i)

    print '###get one dvportgroup########'
    print(vc_object.get_dvportgroups(name='cloud.guest.207.200.1-dvSwitch'))

    print "###get one dvportgroup and the vms associated with it########"
    for vm in vc_object.get_dvportgroups(name='cloud.guest.207.200.1-dvSwitch')[0]['dvportgroup']['vmlist']:
        print(vm.name)
        print(vm.network)

    print '###get multiple dvportgroups########'
    for i in vc_object.get_dvportgroups():
        print(i)

    print vc_object.get_vms(name='VM1')
