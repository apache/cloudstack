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

CREATE TABLE  `cloud_usage`.`usage_vpn_user` (
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `user_name` varchar(32),
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__deleted`(`deleted`);

