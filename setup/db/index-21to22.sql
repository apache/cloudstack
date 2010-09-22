ALTER TABLE `cloud`.`instance_group` ADD CONSTRAINT `fk_instance_group__account_id` FOREIGN KEY `fk_instance_group__account_id` (`account_id`) REFERENCES `account` (`id`);

ALTER TABLE `cloud`.`instance_group_vm_map` ADD CONSTRAINT `fk_instance_group_vm_map___group_id` FOREIGN KEY `fk_instance_group_vm_map___group_id` (`group_id`) REFERENCES `instance_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`instance_group_vm_map` ADD CONSTRAINT `fk_instance_group_vm_map___instance_id` FOREIGN KEY `fk_instance_group_vm_map___instance_id` (`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE;
