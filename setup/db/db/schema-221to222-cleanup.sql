alter table firewall_rules drop column is_static_nat;
delete from configuration where name='router.cleanup';

