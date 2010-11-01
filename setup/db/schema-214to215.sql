ALTER TABLE `cloud`.`storage_pool` MODIFY COLUMN `uuid` varchar(255) UNIQUE;	-- remove NOT NULL constraint to allow delete host/pool
