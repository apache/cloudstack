--;
-- Schema upgrade from 2.2.5 to 2.2.6;
--;

ALTER TABLE `cloud`.`storage_pool` MODIFY `host_address` varchar(255) NOT NULL;


