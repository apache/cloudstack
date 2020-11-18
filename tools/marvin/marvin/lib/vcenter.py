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
import subprocess
import time
import json

if hasattr(ssl, '_create_unverified_context'):
    ssl._create_default_https_context = ssl._create_unverified_context()

class Vcenter():

    def __init__(self, host, user, pwd):
        """
        create a service_instance object
        """
        if hasattr(ssl, '_create_default_https_context'):
            self.service_instance = connect.SmartConnect(host=host,
                                                         user=user,
                                                         pwd=pwd,
                                                         sslContext=ssl._create_default_https_context)
        else:
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
        vm_details['properties'] = {}
        config = obj.config
        if config and config.vAppConfig:
            for prop in config.vAppConfig.property:
                vm_details['properties'][prop.id] = prop.value

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

    def create_datacenter(self, dcname=None, service_instance=None, folder=None):
        """
        Creates a new datacenter with the given name.
        Any % (percent) character used in this name parameter must be escaped,
        unless it is used to start an escape sequence. Clients may also escape
        any other characters in this name parameter.

        An entity name must be a non-empty string of
        less than 80 characters. The slash (/), backslash (\) and percent (%)
        will be escaped using the URL syntax. For example, %2F

        This can raise the following exceptions:
        vim.fault.DuplicateName
        vim.fault.InvalidName
        vmodl.fault.NotSupported
        vmodl.fault.RuntimeFault
        ValueError raised if the name len is > 79
        https://github.com/vmware/pyvmomi/blob/master/docs/vim/Folder.rst

        Required Privileges
        Datacenter.Create

        :param folder: Folder object to create DC in. If None it will default to
                       rootFolder
        :param dcname: Name for the new datacenter.
        :param service_instance: ServiceInstance connection to a given vCenter
        :return:
        """
        if len(dcname) > 79:
            raise ValueError("The name of the datacenter must be under "
                             "80 characters.")

        if folder is None:
            folder = self.service_instance.content.rootFolder

        if folder is not None and isinstance(folder, vim.Folder):
            dc_moref = folder.CreateDatacenter(name=dcname)
            return dc_moref

    def create_cluster(self, cluster_name, datacenter):
        """
        Method to create a Cluster in vCenter

        :param cluster_name: Name of the cluster
        :param datacenter: Name of the data center
        :return: Cluster MORef
        """
        # cluster_name = kwargs.get("name")
        # cluster_spec = kwargs.get("cluster_spec")
        # datacenter = kwargs.get("datacenter")

        if cluster_name is None:
            raise ValueError("Missing value for name.")
        if datacenter is None:
            raise ValueError("Missing value for datacenter.")

        cluster_spec = vim.cluster.ConfigSpecEx()

        host_folder = datacenter.hostFolder
        cluster = host_folder.CreateClusterEx(name=cluster_name, spec=cluster_spec)
        return cluster

    def add_host(self, cluster, hostname, sslthumbprint, username, password):
        """
        Method to add host in a vCenter Cluster

        :param cluster_name
        :param hostname
        :param username
        :param password
        """
        if hostname is None:
            raise ValueError("Missing value for name.")
        try:
            hostspec = vim.host.ConnectSpec(hostName=hostname,
                                            userName=username,
                                            sslThumbprint=sslthumbprint,
                                            password=password,
                                            force=True)
            task = cluster.AddHost(spec=hostspec, asConnected=True)
        except Exception as e:
            print("Error adding host :%s" % e)
        self.wait_for_task(task)
        host = self._get_obj([vim.HostSystem], hostname)
        return host

    def create_datacenters(self, config):
        """
        Method to create data centers in vCenter server programmatically
        It expects configuration data in the form of dictionary.
        configuration file is same as the one we pass to deployDataCenter.py for creating
        datacenter in CS

        :param config:
        :return:
        """
        zones = config['zones']
        try:
            for zone in zones:
                dc_obj = self.create_datacenter(zone['name'])
                for pod in zone['pods']:
                    for cluster in pod['clusters']:
                        clustername = cluster['clustername'].split('/')[-1]
                        cluster_obj = self.create_cluster(
                            cluster_name=clustername,
                            datacenter=dc_obj
                        )
                        for host in cluster['hosts']:
                            host_ip = host['url'].split("//")[-1]
                            user = host['username']
                            passwd = host['password']
                            sslthumbprint=self.getsslThumbprint(host_ip)
                            self.add_host(cluster=cluster_obj,
                                          hostname=host_ip,
                                          sslthumbprint=sslthumbprint,
                                          username=user,
                                          password=passwd)
        except Exception as e:
            print("Failed to create datacenter: %s" % e)

    def wait_for_task(self, task):

        while task.info.state == (vim.TaskInfo.State.running or vim.TaskInfo.State.queued):
            time.sleep(2)

        if task.info.state == vim.TaskInfo.State.success:
            if task.info.result is not None:
                out = 'Task completed successfully, result: %s' % (task.info.result,)
                print(out)
        elif task.info.state == vim.TaskInfo.State.error:
            out = 'Error - Task did not complete successfully: %s' % (task.info.error,)
            raise ValueError(out)
        return task.info.result

    def getsslThumbprint(self,ip):

        p1 = subprocess.Popen(('echo', '-n'), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        p2 = subprocess.Popen(('openssl', 's_client', '-connect', '{0}:443'.format(ip)),
                              stdin=p1.stdout,
                              stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE
        )
        p3 = subprocess.Popen(('openssl', 'x509', '-noout', '-fingerprint', '-sha1'),
                              stdin=p2.stdout,
                              stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE
        )
        out = p3.stdout.read()
        ssl_thumbprint = out.split('=')[-1].strip()
        return ssl_thumbprint
    
if __name__ == '__main__':
    vc_object = Vcenter("10.x.x.x", "username", "password")


    print('###get one dc########')
    print((vc_object.get_datacenters(name='testDC')))

    print('###get multiple dcs########')
    for i in vc_object.get_datacenters():
        print(i)

    print('###get one dv########')
    print(vc_object.get_dvswitches(name='dvSwitch'))

    print('###get multiple dvs########')
    for i in vc_object.get_dvswitches():
        print(i)

    print('###get one dvportgroup########')
    print((vc_object.get_dvportgroups(name='cloud.guest.207.200.1-dvSwitch')))

    print("###get one dvportgroup and the vms associated with it########")
    for vm in vc_object.get_dvportgroups(name='cloud.guest.207.200.1-dvSwitch')[0]['dvportgroup']['vmlist']:
        print((vm.name))
        print((vm.network))

    print('###get multiple dvportgroups########')
    for i in vc_object.get_dvportgroups():
        print(i)

    print(vc_object.get_vms(name='VM1'))
