-- IOPS per GB disk offering
DROP PROCEDURE IF EXISTS `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`;

DELIMITER //
CREATE PROCEDURE `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`(
                        IN tableName VARCHAR(255),
                        IN colName VARCHAR(255),
                        IN colType VARCHAR(255),
                        IN comment VARCHAR(255))
BEGIN
        SET @t1=CONCAT('SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA="cloud" AND TABLE_NAME="', tableName,  '" AND COLUMN_NAME="', colName, '" into @outvar');
        PREPARE stmt1 FROM @t1;
        EXECUTE stmt1;
        DEALLOCATE PREPARE stmt1;
        IF (@outvar < 1) THEN
            SET @t2=CONCAT('ALTER TABLE `cloud`.`', tableName, '` ADD COLUMN `', colName, '` ', colType, ' DEFAULT NULL COMMENT "', comment , '"');
            PREPARE stmt2 FROM @t2;
            EXECUTE stmt2;
            DEALLOCATE PREPARE stmt2;
        END IF; END //
DELIMITER ;

CALL `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`('disk_offering', 'min_iops_per_gb', 'int unsigned', 'Min IOPS per GB for rate based offering');
CALL `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`('disk_offering', 'max_iops_per_gb', 'int unsigned', 'Max IOPS per GB for rate based offering');
CALL `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`('disk_offering', 'highest_min_iops', 'int unsigned', 'Highest Min IOPS value for this offering');
CALL `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`('disk_offering', 'highest_max_iops', 'int unsigned', 'Highest Max IOPS value for this offering');

DROP PROCEDURE `cloud`.`ADD_COLUMN_TO_TABLE_IDEMPOTENT`;

DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
    select
        disk_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.provisioning_type,
        disk_offering.disk_size,
        disk_offering.min_iops,
        disk_offering.max_iops,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.customized,
        disk_offering.customized_iops,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.hv_ss_reserve,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.min_iops_per_gb,
        disk_offering.max_iops_per_gb,
        disk_offering.highest_min_iops,
        disk_offering.highest_max_iops,
        disk_offering.cache_mode,
        disk_offering.sort_key,
        disk_offering.type,
        disk_offering.display_offering,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`disk_offering`
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id
    where
        disk_offering.state='ACTIVE';