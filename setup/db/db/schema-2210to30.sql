--;
-- Schema upgrade from 2.2.10 to 3.0;
--;

ALTER TABLE `cloud`.`template_host_ref` DROP COLUMN `pool_id`;

