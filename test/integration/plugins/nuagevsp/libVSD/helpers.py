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

import logging
import functools
import bambou

LOG = logging.getLogger()


class recreate_session_on_timeout(object):
    def __init__(self, method):
        self.method = method

    def __get__(self, obj=None, objtype=None):
        @functools.wraps(self.method)
        def _wrapper(*args, **kwargs):
            try:
                return self.method(obj, *args, **kwargs)
            except bambou.exceptions.BambouHTTPError as e:
                if e.connection.response.status_code == 401:
                    obj.session = obj.api_client.new_session()
                    return self.method(obj, *args, **kwargs)
                else:
                    raise e

        return _wrapper


class VSDHelpers(object):

    def __init__(self, api_client):
        """
        Create a wrapper
        provide a cspsession and a vpsk object, all from the VSD object
        """
        self.api_client = api_client
        self.session = api_client.session
        self.vspk = api_client.import_vspk(api_client.version)

    def update_vsd_session(self, api_client=None):
        """
        This method is used when Helper is
        initialized before we create a new_session.
        """
        if api_client:
            self.session = api_client.session
            self.vspk = api_client.import_vspk(api_client.version)
        else:
            self.session = self.api_client.session

    @recreate_session_on_timeout
    def add_user_to_group(self, enterprise, user=None, group=None,
                          usr_filter=None, grp_filter=None):
        """
        Add user to a group on VSD.
        For example: Add csproot to cms group
        Here you can couple of things:
        1. enterprise can be id or NURest Object
        2. And User group both need to be NURest object
           or both can be filters.
        """
        if not isinstance(enterprise, self.vspk.NUEnterprise):
            enterprise = self.vspk.NUEnterprise(id=enterprise)
        if isinstance(group, self.vspk.NUGroup):
            if isinstance(user, self.vspk.NUUser):
                all_users = group.users.get()
                all_users.append(user)
                group.assign(all_users, self.vspk.NUUser)
        elif usr_filter and grp_filter:
            group = enterprise.groups.get_first(filter=grp_filter)
            all_users = group.users.get()
            user = enterprise.users.get_first(filter=usr_filter)
            if not group:
                LOG.error('could not fetch the group matching the filter "{}"'
                          .format(grp_filter))
                return
            if not user:
                LOG.error('could not fetch the user matching the filter "{}"'
                          .format(usr_filter))
                return
            all_users.append(user)
            group.assign(all_users, self.vspk.NUUser)

    def set_name_filter(self, name):
        """ set name filter for vsd query
            @param: name:  string name
            @return:  filter string
        """
        return 'name is "{}"'.format(name)

    def set_externalID_filter(self, id):
        """ set externalID filter for vsd query
            @param: id:  string externalID
            @return:  filter string
        """
        return 'externalID is "{}"'.format(id)

    @recreate_session_on_timeout
    def get_enterprise(self, filter):
        """ get_enterprise
            @params: enterprise filter following vspk filter structure
            @return: enterprise object
            @Example:
            self.vsd.get_enterprise(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        enterprise = self.session.user.enterprises.get_first(filter=filter)
        if not enterprise:
            LOG.error('could not fetch the enterprise matching the filter "{}"'
                      .format(filter))
        return enterprise

    @recreate_session_on_timeout
    def get_l2domain(self, enterprise=None, filter=None):
        """ get_l2domain
            @params: enterprise object or enterprise id
                     filter following vspk filter structure
            @return  l2 domain object
            @Example:
            self.vsd.get_l2domain(enterprise=enterprise,
                                filter='name == "{}"'.format(name))
            self.vsd.get_l2domain(enterprise=enterprise_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_l2domain(filter='externalID == "{}"'.format(ext_id))
        """
        l2_domain = None
        if enterprise:
            if not isinstance(enterprise, self.vspk.NUEnterprise):
                enterprise = self.vspk.NUEnterprise(id=enterprise)
            l2_domain = enterprise.l2_domains.get_first(filter=filter)
        elif filter:
            l2_domain = self.session.user.l2_domains.get_first(filter=filter)
        if not l2_domain:
            LOG.error('could not fetch the l2 domain matching the filter "{}"'
                      .format(filter))
        return l2_domain

    @recreate_session_on_timeout
    def get_domain(self, enterprise=None, filter=None):
        """ get_domain
            @params: enterprise object or enterprise id
                     filter following vspk filter structure
            @return: domain object
            @Example:
            self.vsd.get_domain(enterprise=enterprise,
                                filter='name == "{}"'.format(name))
            self.vsd.get_domain(enterprise=enterprise_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_domain(filter='externalID == "{}"'.format(ext_id))
        """
        domain = None
        if enterprise:
            if not isinstance(enterprise, self.vspk.NUEnterprise):
                enterprise = self.vspk.NUEnterprise(id=enterprise)
            domain = enterprise.domains.get_first(filter=filter)
        elif filter:
            domain = self.session.user.domains.get_first(filter=filter)
        if not domain:
            LOG.error('could not fetch the domain matching the filter "{}"'
                      .format(filter))
        return domain

    @recreate_session_on_timeout
    def get_domain_template(self, enterprise=None, filter=None):
        """ get_domain
            @params: enterprise object or enterprise id
                     filter following vspk filter structure
            @return: domain object
            @Example:
            self.vsd.get_domain(enterprise=enterprise,
                                filter='name == "{}"'.format(name))
            self.vsd.get_domain(enterprise=enterprise_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_domain(filter='externalID == "{}"'.format(ext_id))
        """
        domain = None
        if enterprise:
            if not isinstance(enterprise, self.vspk.NUEnterprise):
                enterprise = self.vspk.NUEnterprise(id=enterprise)
            domain = enterprise.domain_templates.get_first(filter=filter)
        elif filter:
            domain = \
                self.session.user.domain_templates.get_first(filter=filter)
        if not domain:
            LOG.error('could not fetch the domain template '
                      'matching the filter "{}"'
                      .format(filter))
        return domain

    @recreate_session_on_timeout
    def get_zone(self, domain=None, filter=None):
        """ get_zone
            @params: domain object or domain id
                     filter following vspk filter structure
            @return: zone object
            @Example:
            self.vsd.get_zone(domain=domain,
                                filter='name == "{}"'.format(name))
            self.vsd.get_zone(domain=domain_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_zone(filter='externalID == "{}"'.format(ext_id))
        """
        zone = None
        if domain:
            if not isinstance(domain, self.vspk.NUDomain):
                domain = self.vspk.NUDomain(id=domain)
            zone = domain.zones.get_first(filter=filter)
        elif filter:
            zone = self.session.user.zones.get_first(filter=filter)
        if not zone:
            LOG.error('could not fetch the zone matching the filter "{}"'
                      .format(filter))
        return zone

    @recreate_session_on_timeout
    def get_subnet(self, zone=None, filter=None):
        """ get_subnet
            @params: zone object or zone id
                     filter following vspk filter structure
            @return: subnet object
            @Example:
            self.vsd.get_subnet(zone=zone,
                                filter='name == "{}"'.format(name))
            self.vsd.get_subnet(zone=zone_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_subnet(filter='externalID == "{}"'.format(ext_id))
        """
        subnet = None
        if zone:
            if not isinstance(zone, self.vspk.NUZone):
                zone = self.vspk.NUZone(id=zone)
            subnet = zone.subnets.get_first(filter=filter)
        elif filter:
            subnet = self.session.user.subnets.get_first(filter=filter)
        if not subnet:
            LOG.error('could not fetch the subnet matching the filter "{}"'
                      .format(filter))
        return subnet

    @recreate_session_on_timeout
    def get_subnet_from_domain(self, domain=None, filter=None):
        """ get_subnet
            @params: domain object or domain id
                     filter following vspk filter structure
            @return: subnet object
            @Example:
            self.vsd.get_subnet(domain=domain,
                                filter='name == "{}"'.format(name))
            self.vsd.get_subnet(domain=domain_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_subnet(filter='externalID == "{}"'.format(ext_id))
        """
        subnet = None
        if domain:
            if not isinstance(domain, self.vspk.NUDomain):
                domain = self.vspk.NUDomain(id=domain)
            subnet = domain.subnets.get_first(filter=filter)
        elif filter:
            subnet = self.session.user.subnets.get_first(filter=filter)
        if not subnet:
            LOG.error('could not fetch the subnet matching the filter "{}"'
                      .format(filter))
        return subnet

    @recreate_session_on_timeout
    def get_vm(self, subnet=None, filter=None):
        """ get_vm
            @params: subnet object or subnet id
                     filter following vspk filter structure
            @return: vm object
            @Example:
            self.vsd.get_vm(subnet=subnet,
                                filter='name == "{}"'.format(name))
            self.vsd.get_vm(subnet=subnet_id,
                               filter='name == "{}"'.format(name))
            self.vsd.get_vm(filter='externalID == "{}"'.format(ext_id))
        """
        vm = None
        if subnet:
            if not isinstance(subnet, self.vspk.NUSubnet):
                subnet = self.vspk.NUSubnet(id=subnet)
            vm = subnet.vms.get_first(filter=filter)
        elif filter:
            vm = self.session.user.vms.get_first(filter=filter)
        if not vm:
            LOG.error('could not fetch the vm matching the filter "{}"'
                      .format(filter))
        return vm

    @recreate_session_on_timeout
    def get_subnet_dhcpoptions(self, subnet=None, filter=None):
        """ get_subnet_dhcpoptions
            @params: subnet object or
                     subnet filter following vspk filter structure
            @return: subnet dhcpoptions object
            @Example:
            self.vsd.get_subnet_dhcpoptions(subnet=subnet)
            self.vsd.get_subnet_dhcpoptions(
                filter='externalID == "{}"'.format(subnet_externalID))
        """
        if not isinstance(subnet, self.vspk.NUSubnet):
            if not filter:
                LOG.error('a filter is required')
                return None
            subnet = self.session.user.subnets.get_first(filter=filter)
        dhcp_options = subnet.dhcp_options.get()
        if not dhcp_options:
            if filter:
                LOG.error('could not fetch the dhcp options on the subnet '
                          'matching the filter "{}"'
                          .format(filter))
            else:
                LOG.error('could not fetch the dhcp options on the subnet')
        return dhcp_options

    @recreate_session_on_timeout
    def get_vport(self, subnet, filter):
        """ get_vport
            @params: subnet object
                     vport filter following vspk filter structure
            @return: vport object
            @Example:
            self.vsd.get_vport(subnet=subnet,
                filter='externalID == "{}"'.format(ext_id))
        """
        if not isinstance(subnet, self.vspk.NUSubnet):
            LOG.error('a subnet is required')
            return None
        if not filter:
            LOG.error('a filter is required')
            return None
        vport = subnet.vports.get_first(filter=filter)
        if not vport:
            LOG.error('could not fetch the vport from the subnet '
                      'matching the filter "{}"'
                      .format(filter))
        return vport

    @recreate_session_on_timeout
    def get_vm_interface(self, filter):
        """ get_vm_interface
            @params: vm interface filter following vspk filter structure
            @return: vm interface object
            @Example:
            self.vsd.get_vm_interface(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        vm_interface = self.session.user.vm_interfaces.get_first(filter=filter)
        if not vm_interface:
            LOG.error('could not fetch the vm interface '
                      'matching the filter "{}"'
                      .format(filter))
        return vm_interface

    @recreate_session_on_timeout
    def get_vm_interface_policydecisions(self, vm_interface=None, filter=None):
        """ get_vm_interface_policydecisions
            @params: vm interface object or
                     vm interface filter following vspk filter structure
            @return: vm interface policydecisions object
            @Example:
            self.vsd.get_vm_interface_policydecisions(vm_interface=interface)
            self.vsd.get_vm_interface_policydecisions(
                filter='externalID == "{}"'.format(vm_interface_externalID))
        """
        if not isinstance(vm_interface, self.vspk.NUVMInterface):
            if not filter:
                LOG.error('a filter is required')
                return None
            vm_interface = \
                self.session.user.vm_interfaces.get_first(filter=filter)
        policy_decisions = self.vspk.NUPolicyDecision(
            id=vm_interface.policy_decision_id).fetch()
        if not policy_decisions:
            if filter:
                LOG.error('could not fetch the policy decisions on the '
                          'vm interface matching the filter "{}"'
                          .format(filter))
            else:
                LOG.error('could not fetch the policy decisions '
                          'on the vm interface')
        return policy_decisions

    @recreate_session_on_timeout
    def get_vm_interface_dhcpoptions(self, vm_interface=None, filter=None):
        """ get_vm_interface_dhcpoptions
            @params: vm interface object or
                     vm interface filter following vspk filter structure
            @return: vm interface dhcpoptions object
            @Example:
            self.vsd.get_vm_interface_dhcpoptions(vm_interface=vm_interface)
            self.vsd.get_vm_interface_dhcpoptions(
                filter='externalID == "{}"'.format(vm_interface_externalID))
        """
        if not isinstance(vm_interface, self.vspk.NUVMInterface):
            if not filter:
                LOG.error('a filter is required')
                return None
            vm_interface = self.session.user.vm_interfaces.get_first(
                    filter=filter)
        dhcp_options = vm_interface.dhcp_options.get()
        if not dhcp_options:
            if filter:
                LOG.error('could not fetch the dhcp options on the '
                          'vm interface matching the filter "{}"'
                          .format(filter))
            else:
                LOG.error('could not fetch the dhcp options on the '
                          'vm interface')
        return dhcp_options

    @recreate_session_on_timeout
    def get_ingress_acl_entry(self, filter):
        """ get_ingress_acl_entry
            @params: ingress acl entry filter following vspk filter structure
            @return: ingress acl entry object
            @Example:
            self.vsd.get_ingress_acl_entry(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        acl = self.session.user.ingress_acl_entry_templates.get_first(
                filter=filter)
        if not acl:
            LOG.error('could not fetch the ingress acl entry '
                      'matching the filter "{}"'
                      .format(filter))
        return acl

    @recreate_session_on_timeout
    def get_egress_acl_entry(self, filter):
        """ get_egress_acl_entry
            @params: egress acl entry filter following vspk filter structure
            @return: egress acl entry object
            @Example:
            self.vsd.get_egress_acl_entry(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        acl = self.session.user.egress_acl_entry_templates.get_first(
                filter=filter)
        if not acl:
            LOG.error('could not fetch the egress acl entry '
                      'matching the filter "{}"'
                      .format(filter))
        return acl

    @recreate_session_on_timeout
    def get_qoss(self, vport):
        """ get_qoss
            @params: vport object
            @return: qoss object
            @Example:
            self.vsd.get_qoss(vport=vport)
        """
        if not isinstance(vport, self.vspk.NUVPort):
            LOG.error('a vport is required')
            return None
        qoss = vport.qoss.get()
        if not qoss:
            LOG.error('could not fetch the qoss from the vport')
        return qoss

    @recreate_session_on_timeout
    def get_floating_ip(self, filter):
        """ get_floating_ip
            @params: floating ip filter following vspk filter structure
            @return: floating ip object
            @Example:
            self.vsd.get_floating_ip(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        floating_ip = self.session.user.floating_ips.get_first(filter=filter)
        if not floating_ip:
            LOG.error('could not fetch the floating ip '
                      'matching the filter "{}"'
                      .format(filter))
        return floating_ip

    @recreate_session_on_timeout
    def get_ingress_acl_entries(self, filter):
        """ get_ingress_acl_entries
            @params: ingress acl entries (templates) filter following vspk
                     filter structure
            @return: ingress acl entries (objects) list
            @Example:
            self.vsd.get_ingress_acl_entries(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        templates = self.session.user.ingress_acl_templates.get(filter=filter)
        if not templates:
            LOG.error('could not fetch the ingress acl entries (templates) '
                      'matching the filter "{}"'
                      .format(filter))
            return None
        acls = []
        for template in templates:
            tmp = self.vspk.NUIngressACLTemplate(id=template.id)
            acl = tmp.ingress_acl_entry_templates.get()
            acls.append(acl)
        return acls

    @recreate_session_on_timeout
    def get_egress_acl_entries(self, filter):
        """ get_egress_acl_entries
            @params: egress acl entries (templates) filter
                     following vspk filter structure
            @return: egress acl entries (objects) list
            @Example:
            self.vsd.get_egress_acl_entries(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        templates = self.session.user.egress_acl_templates.get(filter=filter)
        if not templates:
            LOG.error('could not fetch the egress acl entries (templates) '
                      'matching the filter "{}"'
                      .format(filter))
            return None
        acls = []
        for template in templates:
            tmp = self.vspk.NUEgressACLTemplate(id=template.id)
            acl = tmp.egress_acl_entry_templates.get()
            acls.append(acl)
        return acls

    @recreate_session_on_timeout
    def get_shared_network_resource(self, filter):
        """ get_shared_network_resource
            @params: shared network resource filter
                    following vspk filter structure
            @return: shared network resource object
            @Example:
            self.vsd.get_shared_network_resource(
                filter='externalID == "{}"'.format(ext_id))
        """
        if not filter:
            LOG.error('a filter is required')
            return None
        shared_network_resource = \
            self.session.user.shared_network_resources.get_first(filter=filter)
        if not shared_network_resource:
            LOG.error('could not fetch the shared network resource '
                      'matching the filter "{}"'
                      .format(filter))
        return shared_network_resource

    @recreate_session_on_timeout
    def get_virtualip(self, vport, filter):
        """ get_virtualip
            @params: vport object
                     virtualip filter following vspk filter structure
            @return: virtualip object
            @Example:
            self.vsd.get_virtualip(vport=vport,
                filter='externalID == "{}"'.format(ext_id))
        """
        if not isinstance(vport, self.vspk.NUVPort):
            LOG.error('a vport is required')
            return None
        if not filter:
            LOG.error('a filter is required')
            return None
        virtualip = vport.virtual_ips.get_first(filter=filter)

        if not virtualip:
            LOG.error('could not fetch the virtualip matching the filter "{}"'
                      .format(filter))
        return virtualip
