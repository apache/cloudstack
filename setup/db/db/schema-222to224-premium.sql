--;
-- Schema upgrade from 2.2.2 to 2.2.4;
--;
ALTER TABLE `cloud_usage`.`usage_vm_instance` ADD COLUMN `hypervisor_type` varchar(255);

ALTER TABLE `cloud_usage`.`usage_event` ADD COLUMN `resource_type` varchar(32);

