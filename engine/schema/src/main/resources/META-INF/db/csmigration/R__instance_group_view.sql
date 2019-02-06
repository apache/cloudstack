DROP VIEW IF EXISTS `cloud`.`instance_group_view`;
CREATE VIEW `cloud`.`instance_group_view` AS
    select
        instance_group.id,
        instance_group.uuid,
        instance_group.name,
        instance_group.removed,
        instance_group.created,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    from
        `cloud`.`instance_group`
            inner join
        `cloud`.`account` ON instance_group.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = instance_group.account_id;