--;
-- Schema upgrade from 2.2.4 to 2.2.5;
--;

ALTER TABLE `cloud`.`security_group` add UNIQUE KEY (`name`, `account_id`);
