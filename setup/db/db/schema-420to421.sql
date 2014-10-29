-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from 4.2.0 to 4.2.1;
--;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 's3.singleupload.max.size', '5', 
    'The maximum size limit for S3 single part upload API(in GB). If it is set to 0, then it means always use multi-part upload to upload object to S3. If it is set to -1, then it means always use single-part upload to upload object to S3.');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Storage", 'DEFAULT', 'management-server', "enable.ha.storage.migration", "true", "Enable/disable storage migration across primary storage during HA"); 
UPDATE `cloud`.`configuration` SET description="Specify whether or not to reserve CPU based on CPU overprovisioning factor" where name="vmware.reserve.cpu";
UPDATE `cloud`.`configuration` SET description="Specify whether or not to reserve memory based on memory overprovisioning factor" where name="vmware.reserve.mem";
-- Remove Windows Server 8 from guest_os_type dropdown to use Windows Server 2012
DELETE FROM `cloud`.`guest_os_hypervisor` where guest_os_id=168;
DELETE FROM `cloud`.`guest_os` where id=168;


--Add details to the user_vm_view (CLOUDSTACK-4649 - xen and UI code needs to retrieve the value of "hypervisortoolsversion" detail)

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE VIEW `cloud`.`user_vm_view` AS
    select
        vm_instance.id id,
        vm_instance.name name,
        user_vm.display_name display_name,
        user_vm.user_data user_data,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        instance_group.id instance_group_id,
        instance_group.uuid instance_group_uuid,
        instance_group.name instance_group_name,
        vm_instance.uuid uuid,
        vm_instance.last_host_id last_host_id,
        vm_instance.vm_type type,
        vm_instance.vnc_password vnc_password,
        vm_instance.limit_cpu_use limit_cpu_use,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.ha_enabled ha_enabled,
        vm_instance.hypervisor_type hypervisor_type,
        vm_instance.instance_name instance_name,
        vm_instance.guest_os_id guest_os_id,
        vm_instance.display_vm display_vm,
        guest_os.uuid guest_os_uuid,
        vm_instance.pod_id pod_id,
        host_pod_ref.uuid pod_uuid,
        vm_instance.private_ip_address private_ip_address,
        vm_instance.private_mac_address private_mac_address,
        vm_instance.vm_type vm_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.is_security_group_enabled security_group_enabled,
        data_center.networktype data_center_type,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        vm_template.enable_password password_enabled,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        Case
             When (`cloud`.`service_offering`.`cpu` is null) then (`custom_cpu`.`value`)
             Else ( `cloud`.`service_offering`.`cpu`)
        End as `cpu`,
        Case
            When (`cloud`.`service_offering`.`speed` is null) then (`custom_speed`.`value`)
            Else ( `cloud`.`service_offering`.`speed`)
        End as `speed`,
        Case
            When (`cloud`.`service_offering`.`ram_size` is null) then (`custom_ram_size`.`value`)
            Else ( `cloud`.`service_offering`.`ram_size`)
        END as `ram_size`,
        disk_offering.name service_offering_name,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.pool_type pool_type,
        volumes.id volume_id,
        volumes.uuid volume_uuid,
        volumes.device_id volume_device_id,
        volumes.volume_type volume_type,
        security_group.id security_group_id,
        security_group.uuid security_group_uuid,
        security_group.name security_group_name,
        security_group.description security_group_description,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        user_ip_address.id public_ip_id,
        user_ip_address.uuid public_ip_uuid,
        user_ip_address.public_ip_address public_ip_address,
        ssh_keypairs.keypair_name keypair_name,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        affinity_group.id affinity_group_id,
        affinity_group.uuid affinity_group_uuid,
        affinity_group.name affinity_group_name,
        affinity_group.description affinity_group_description,
        vm_instance.dynamically_scalable dynamically_scalable,
        all_details.name detail_name,
        all_details.value detail_value

    from
        `cloud`.`user_vm`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = user_vm.id
            and vm_instance.removed is NULL
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`guest_os` ON vm_instance.guest_os_id = guest_os.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`instance_group_vm_map` ON vm_instance.id = instance_group_vm_map.instance_id
            left join
        `cloud`.`instance_group` ON instance_group_vm_map.group_id = instance_group.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = user_vm.iso_id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`security_group_vm_map` ON vm_instance.id = security_group_vm_map.instance_id
            left join
        `cloud`.`security_group` ON security_group_vm_map.security_group_id = security_group.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id and nics.removed is null
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON networks.vpc_id = vpc.id and vpc.removed is null
            left join
        `cloud`.`user_ip_address` ON user_ip_address.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` as ssh_details ON ssh_details.vm_id = vm_instance.id
            and ssh_details.name = 'SSH.PublicKey'
            left join
        `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = ssh_details.value
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_instance.id
            and resource_tags.resource_type = 'UserVm'
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'VirtualMachine'
            and async_job.job_status = 0
            left join
        `cloud`.`affinity_group_vm_map` ON vm_instance.id = affinity_group_vm_map.instance_id
            left join
        `cloud`.`affinity_group` ON affinity_group_vm_map.affinity_group_id = affinity_group.id
            left join
        `cloud`.`user_vm_details` as all_details ON all_details.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` `custom_cpu`  ON (((`custom_cpu`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_cpu`.`name` = 'CpuNumber')))
            left join
        `cloud`.`user_vm_details` `custom_speed`  ON (((`custom_speed`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_speed`.`name` = 'CpuSpeed')))
           left join
        `cloud`.`user_vm_details` `custom_ram_size`  ON (((`custom_ram_size`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_ram_size`.`name` = 'memory')));

--Add the format for volumes table for uploaded volumes (CLOUDSTACK-5013)
update  `cloud`.`volumes` v,  `cloud`.`volume_host_ref` vhr  set v.format=vhr.format where v.id=vhr.volume_id and v.format is null;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'baremetal.ipmi.lan.interface', 'default', 'option specified in -I option of impitool. candidates are: open/bmc/lipmi/lan/lanplus/free/imb, see ipmitool man page for details. default valule "default" means using default option of ipmitool');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'baremetal.ipmi.fail.retry', 'default', "ipmi interface will be temporary out of order after power opertions(e.g. cycle, on), it leads following commands fail immediately. The value specifies retry times before accounting it as real failure");
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.hung.wokervm.timeout', '7200', 'Worker VM timeout in seconds');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Alert", 'DEFAULT', 'management-server', "alert.smtp.connectiontimeout", "30000", "Socket connection timeout value in milliseconds. -1 for infinite timeout.");
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Alert", 'DEFAULT', 'management-server', "alert.smtp.timeout", "30000", "Socket I/O timeout value in milliseconds. -1 for infinite timeout.");
