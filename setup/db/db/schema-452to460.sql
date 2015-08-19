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

--
-- Schema upgrade from 4.5.1 to 4.6.0
--

ALTER TABLE `cloud`.`snapshots` ADD COLUMN `min_iops` bigint(20) unsigned COMMENT 'Minimum IOPS';
ALTER TABLE `cloud`.`snapshots` ADD COLUMN `max_iops` bigint(20) unsigned COMMENT 'Maximum IOPS';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "stats.output.uri", "", "URI to additionally send StatsCollector statistics to", "", NULL, NULL, 0);

DROP VIEW IF EXISTS `cloud`.`domain_view`;
CREATE VIEW `cloud`.`domain_view` AS
    select
        domain.id id,
        domain.parent parent,
        domain.name name,
        domain.uuid uuid,
        domain.owner owner,
        domain.path path,
        domain.level level,
        domain.child_count child_count,
        domain.next_child_seq next_child_seq,
        domain.removed removed,
        domain.state state,
        domain.network_domain network_domain,
        domain.type type,
        vmlimit.max vmLimit,
        vmcount.count vmTotal,
        iplimit.max ipLimit,
        ipcount.count ipTotal,
        volumelimit.max volumeLimit,
        volumecount.count volumeTotal,
        snapshotlimit.max snapshotLimit,
        snapshotcount.count snapshotTotal,
        templatelimit.max templateLimit,
        templatecount.count templateTotal,
        vpclimit.max vpcLimit,
        vpccount.count vpcTotal,
        projectlimit.max projectLimit,
        projectcount.count projectTotal,
        networklimit.max networkLimit,
        networkcount.count networkTotal,
        cpulimit.max cpuLimit,
        cpucount.count cpuTotal,
        memorylimit.max memoryLimit,
        memorycount.count memoryTotal,
        primary_storage_limit.max primaryStorageLimit,
        primary_storage_count.count primaryStorageTotal,
        secondary_storage_limit.max secondaryStorageLimit,
        secondary_storage_count.count secondaryStorageTotal
    from
        `cloud`.`domain`
            left join
        `cloud`.`resource_limit` vmlimit ON domain.id = vmlimit.domain_id
            and vmlimit.type = 'user_vm'
            left join
        `cloud`.`resource_count` vmcount ON domain.id = vmcount.domain_id
            and vmcount.type = 'user_vm'
            left join
        `cloud`.`resource_limit` iplimit ON domain.id = iplimit.domain_id
            and iplimit.type = 'public_ip'
            left join
        `cloud`.`resource_count` ipcount ON domain.id = ipcount.domain_id
            and ipcount.type = 'public_ip'
            left join
        `cloud`.`resource_limit` volumelimit ON domain.id = volumelimit.domain_id
            and volumelimit.type = 'volume'
            left join
        `cloud`.`resource_count` volumecount ON domain.id = volumecount.domain_id
            and volumecount.type = 'volume'
            left join
        `cloud`.`resource_limit` snapshotlimit ON domain.id = snapshotlimit.domain_id
            and snapshotlimit.type = 'snapshot'
            left join
        `cloud`.`resource_count` snapshotcount ON domain.id = snapshotcount.domain_id
            and snapshotcount.type = 'snapshot'
            left join
        `cloud`.`resource_limit` templatelimit ON domain.id = templatelimit.domain_id
            and templatelimit.type = 'template'
            left join
        `cloud`.`resource_count` templatecount ON domain.id = templatecount.domain_id
            and templatecount.type = 'template'
            left join
        `cloud`.`resource_limit` vpclimit ON domain.id = vpclimit.domain_id
            and vpclimit.type = 'vpc'
            left join
        `cloud`.`resource_count` vpccount ON domain.id = vpccount.domain_id
            and vpccount.type = 'vpc'
            left join
        `cloud`.`resource_limit` projectlimit ON domain.id = projectlimit.domain_id
            and projectlimit.type = 'project'
            left join
        `cloud`.`resource_count` projectcount ON domain.id = projectcount.domain_id
            and projectcount.type = 'project'
            left join
        `cloud`.`resource_limit` networklimit ON domain.id = networklimit.domain_id
            and networklimit.type = 'network'
            left join
        `cloud`.`resource_count` networkcount ON domain.id = networkcount.domain_id
            and networkcount.type = 'network'
            left join
        `cloud`.`resource_limit` cpulimit ON domain.id = cpulimit.domain_id
            and cpulimit.type = 'cpu'
            left join
        `cloud`.`resource_count` cpucount ON domain.id = cpucount.domain_id
            and cpucount.type = 'cpu'
            left join
        `cloud`.`resource_limit` memorylimit ON domain.id = memorylimit.domain_id
            and memorylimit.type = 'memory'
            left join
        `cloud`.`resource_count` memorycount ON domain.id = memorycount.domain_id
            and memorycount.type = 'memory'
            left join
        `cloud`.`resource_limit` primary_storage_limit ON domain.id = primary_storage_limit.domain_id
            and primary_storage_limit.type = 'primary_storage'
            left join
        `cloud`.`resource_count` primary_storage_count ON domain.id = primary_storage_count.domain_id
            and primary_storage_count.type = 'primary_storage'
            left join
        `cloud`.`resource_limit` secondary_storage_limit ON domain.id = secondary_storage_limit.domain_id
            and secondary_storage_limit.type = 'secondary_storage'
            left join
        `cloud`.`resource_count` secondary_storage_count ON domain.id = secondary_storage_count.domain_id
            and secondary_storage_count.type = 'secondary_storage';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.user.vms','-1','The default maximum number of user VMs that can be deployed for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.public.ips','-1','The default maximum number of public IPs that can be consumed by a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.templates','-1','The default maximum number of templates that can be deployed for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.snapshots','-1','The default maximum number of snapshots that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.volumes','-1','The default maximum number of volumes that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.networks', '-1', 'The default maximum number of networks that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.vpcs', '-1', 'The default maximum number of vpcs that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.cpus', '-1', 'The default maximum number of cpu cores that can be used for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.memory', '-1', 'The default maximum memory (in MiB) that can be used for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.primary.storage', '-1', 'The default maximum primary storage space (in GiB) that can be used for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.secondary.storage', '-1', 'The default maximum secondary storage space (in GiB) that can be used for a domain', '-1', NULL, NULL, 0);

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `user_id` bigint unsigned NOT NULL DEFAULT 1 COMMENT 'user id of VM deployer';

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
        vm_instance.user_id user_id,
        vm_instance.last_host_id last_host_id,
        vm_instance.vm_type type,
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
        svc_disk_offering.uuid service_offering_uuid,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.id disk_offering_id,
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
        svc_disk_offering.name service_offering_name,
        disk_offering.name disk_offering_name,
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
        vm_instance.dynamically_scalable dynamically_scalable

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
        `cloud`.`disk_offering` svc_disk_offering ON vm_instance.service_offering_id = svc_disk_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.disk_offering_id = disk_offering.id
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
        `cloud`.`user_vm_details` `custom_cpu`  ON (((`custom_cpu`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_cpu`.`name` = 'CpuNumber')))
            left join
        `cloud`.`user_vm_details` `custom_speed`  ON (((`custom_speed`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_speed`.`name` = 'CpuSpeed')))
           left join
        `cloud`.`user_vm_details` `custom_ram_size`  ON (((`custom_ram_size`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_ram_size`.`name` = 'memory')));

-- ovm3 stuff
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 10(32-bit)', 79);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 10(64-bit)', 80);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 11(32-bit)', 158);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 11(64-bit)', 159);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Other Linux (32-bit)', 98);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Other Linux (64-bit)', 99);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('Ovm3', 'Other PV (32-bit)', 139);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('Ovm3', 'Other PV (64-bit)', 140);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('Ovm3', 'DOS', 102);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Windows 8 (32-bit)', 165);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Windows 8 (64-bit)', 166);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Windows Server 2012 (64-bit)', 167);

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('Ovm3', '3.2', 25, 0);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('Ovm3', '3.3', 50, 0);
UPDATE  `cloud`.`volumes` v,  `cloud`.`storage_pool` s,  `cloud`.`cluster` c  set v.format='RAW' where v.pool_id=s.id and s.cluster_id=c.id and c.hypervisor_type='Ovm3';
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm,Ovm3,LXC' WHERE name='hypervisor.list';
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id,featured, cross_zones, hypervisor_type, state)
VALUES (12, UUID(), 'routing-12', 'SystemVM Template (Ovm3)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloud.com/templates/4.6/systemvm64template.ovm.raw.bz2', '4425688804dbcf0abc9e9e56c53070d7', 0, 'SystemVM Template (Ovm3)', 'RAW', 183, 0, 1, 'Ovm3', 'Active' );

INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'ManagementServer', 'ovm3.heartbeat.timeout' , '180', '120', 'Timeout value to send to the checkheartbeat script for guarding the self fencing functionality on ovm3');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'ManagementServer', 'ovm3.heartbeat.interval' , '10', '1', 'Interval value the checkheartbeat script uses before triggering the timeout for ovm3');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.ovm3', 'SystemVM Template (Ovm3)', 'Name of the default router template on Ovm3.','SystemVM Template (Ovm3)', NULL, NULL, 0);

UPDATE IGNORE `cloud`.`configuration` SET `value`="PLAINTEXT" WHERE `name`="user.authenticators.exclude";

DROP TABLE IF EXISTS `cloud`.`external_bigswitch_vns_devices`;
CREATE TABLE `cloud`.`external_bigswitch_bcf_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which bigswitch bcf device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this bigswitch bcf device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the bigswitch bcf device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external bigswitch bcf device',
  `hostname` varchar(255) NOT NULL COMMENT 'host name or IP address for the bigswitch bcf device',
  `username` varchar(255) NOT NULL COMMENT 'username for the bigswitch bcf device',
  `password` varchar(255) NOT NULL COMMENT 'password for the bigswitch bcf device',
  `nat` boolean NOT NULL COMMENT 'NAT support for the bigswitch bcf device',
  `hash` varchar(255) NOT NULL COMMENT 'topology hash for the bigswitch bcf networks',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_external_bigswitch_bcf_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_bigswitch_bcf_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

