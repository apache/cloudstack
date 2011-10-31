--;
-- Premium Schema upgrade from 2.2.13 to 3.0;
--;

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';

ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';

update `cloud_usage`.`usage_network` set agg_bytes_received = net_bytes_received + current_bytes_received, agg_bytes_sent = net_bytes_sent + current_bytes_sent;

ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_sent`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_sent`;

