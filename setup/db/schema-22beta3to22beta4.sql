CREATE TABLE `cloud`.`host_tags` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL COMMENT 'host id',
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`host_tags` ADD CONSTRAINT `fk_host_tags__host_id` FOREIGN KEY `fk_host_tags__host_id`(`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `host_tag` varchar(255);