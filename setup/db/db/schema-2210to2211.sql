--;
-- Schema upgrade from 2.2.10 to 2.2.11;
--;

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `template_tag` varchar(255) COMMENT 'template tag';

