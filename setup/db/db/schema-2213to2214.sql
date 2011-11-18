--;
-- Schema upgrade from 2.2.13 to 2.2.14;
--;

ALTER TABLE `cloud`.`vm_template` MODIFY `extractable` int(1) unsigned NOT NULL default 0 COMMENT 'Is this template extractable';