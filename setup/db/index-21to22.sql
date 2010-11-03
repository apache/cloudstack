ALTER TABLE `cloud`.`instance_group` ADD CONSTRAINT `fk_instance_group__account_id` FOREIGN KEY `fk_instance_group__account_id` (`account_id`) REFERENCES `account` (`id`);

ALTER TABLE `cloud`.`instance_group_vm_map` ADD CONSTRAINT `fk_instance_group_vm_map___group_id` FOREIGN KEY `fk_instance_group_vm_map___group_id` (`group_id`) REFERENCES `instance_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`instance_group_vm_map` ADD CONSTRAINT `fk_instance_group_vm_map___instance_id` FOREIGN KEY `fk_instance_group_vm_map___instance_id` (`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`remote_access_vpn` ADD CONSTRAINT `fk_remote_access_vpn___account_id` FOREIGN KEY `fk_remote_access_vpn__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`remote_access_vpn` ADD CONSTRAINT `fk_remote_access_vpn__zone_id` FOREIGN KEY `fk_remote_access_vpn__zone_id` (`zone_id`) REFERENCES `data_center` (`id`);
ALTER TABLE `cloud`.`remote_access_vpn` ADD CONSTRAINT `fk_remote_access_vpn__server_addr` FOREIGN KEY `fk_remote_access_vpn__server_addr` (`vpn_server_addr`) REFERENCES `user_ip_address` (`public_ip_address`);
ALTER TABLE `cloud`.`remote_access_vpn` ADD INDEX `i_remote_access_vpn_addr`(`vpn_server_addr`);

ALTER TABLE `cloud`.`vpn_users` ADD CONSTRAINT `fk_vpn_users___account_id` FOREIGN KEY `fk_vpn_users__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`vpn_users` ADD INDEX `i_vpn_users_username`(`username`);
ALTER TABLE `cloud`.`vpn_users` ADD UNIQUE `i_vpn_users__account_id__username`(`account_id`, `username`); 
