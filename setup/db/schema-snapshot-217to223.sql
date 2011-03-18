ALTER table snapshots add column `data_center_id` bigint unsigned NOT NULL ;
ALTER table snapshots add column `domain_id` bigint unsigned NOT NULL;
ALTER table snapshots add column `disk_offering_id` bigint unsigned NOT NULL;
ALTER table snapshots add column `size` bigint unsigned NOT NULL;
ALTER table snapshots add column `version` varchar(32) DEFAULT '2.1';
ALTER table snapshots add column `hypervisor_type` varchar(32) DEFAULT 'XenServer';

UPDATE snapshots s, volumes v SET s.data_center_id=v.data_center_id, s.domain_id=v.domain_id, s.disk_offering_id=v.disk_offering_id, s.size=v.size WHERE s.volume_id = v.id;
UPDATE snapshots s, snapshot_policy sp, snapshot_policy_ref spr SET s.hypervisor_type=sp.interval+3 WHERE s.id=spr.snap_id and spr.policy_id=sp.id;

DROP table snapshot_policy_ref;
DELETE FROM snapshot_policy WHERE id=1;