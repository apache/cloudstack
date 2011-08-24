--;
-- Schema upgrade from 2.2.1 to 2.2.2;
--;
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `network_id` bigint unsigned;

ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `network_id` bigint unsigned;

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `network_id` bigint unsigned;
ALTER TABLE `cloud_usage`.`user_statistics` ADD UNIQUE KEY (`account_id`, `data_center_id`, `public_ip_address`, `device_id`, `device_type`);
