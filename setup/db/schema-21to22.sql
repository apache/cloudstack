--
-- Schema upgrade from 2.1 to 2.2
--

ALTER TABLE `cloud`.`data_center` MODIFY COLUMN `guest_network_cidr` varchar(18); -- modify column width to 18 from 15

ALTER TABLE `cloud`.`resource_count` ADD COLUMN `domain_id` bigint unsigned; -- add the new column
ALTER TABLE `cloud`.`resource_count` MODIFY COLUMN `account_id` bigint unsigned; -- modify the column to allow NULL values
ALTER TABLE `cloud`.`storage_pool` add COLUMN STATUS varchar(32) not null; -- new status column for maintenance mode support for primary storage
ALTER TABLE `cloud`.`volumes` ADD COLUMN `source_id` bigint unsigned;  -- id for the source
ALTER TABLE `cloud`.`volumes` ADD COLUMN `source_type` varchar(32); --source from which the volume is created i.e. snapshot, diskoffering, template, blank
