alter table cloud_usage add column `network_id` bigint unsigned;
alter table usage_network add column `network_id` bigint unsigned;
alter table user_statistics add column `network_id` bigint unsigned;
