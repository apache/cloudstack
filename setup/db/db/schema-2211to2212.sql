--;
-- Schema upgrade from 2.2.11 to 2.2.12;
--;

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `template_tag` varchar(255) COMMENT 'template tag';

