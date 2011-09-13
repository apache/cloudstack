--;
-- Schema upgrade from 2.2.11 to 2.2.12;
--;

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `template_tag` varchar(255) COMMENT 'template tag';

UPDATE vm_instance SET state='Error' WHERE state='Creating' AND removed IS null;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'json.content.type', 'text/javascript', 'Http response content type for .js files (default is text/javascript)');
INSERT IGNORE INTO configuration VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.aggregation.timezone', 'GMT', 'The timezone to use for usage stats aggregation');

ALTER IGNORE TABLE `cloud`.`user_vm_details` DROP FOREIGN KEY `fk_user_vm_details__vm_id`;
ALTER TABLE `cloud`.`user_vm_details` ADD CONSTRAINT `fk_user_vm_details__vm_id` FOREIGN KEY `fk_user_vm_details__vm_id`(`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;

DELETE FROM `cloud`.`configuration` where name='vmware.guest.nic.device.type';

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'agent.lb.enabled', 'true', 'If agent load balancing enabled in cluster setup');

update `cloud`.`vm_template` set hypervisor_type='XenServer' where unique_name='xs-tools.iso';

ALTER TABLE `cloud`.`host` MODIFY COLUMN  `storage_ip_address` char(40);

