ALTER TABLE `cloud`.`vm_instance` MODIFY COLUMN `pod_id` bigint unsigned;	-- remove NOT NULL constraint to allow creating DB record in early time
