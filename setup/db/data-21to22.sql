--data upgrade from 21 to 22
use cloud;

START TRANSACTION;
DELETE FROM template_host_ref WHERE install_path LIKE '%xs-tools%'
DELETE FROM configuration where name='upgrade.url';
DELETE FROM configuration where name='router.template.id';
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.client.iprange', '10.1.2.1-10.1.2.8', 'The range of ips to be allocated to remote access vpn clients. The first ip in the range is used by the VPN server');
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.psk.length', '48', 'The length of the ipsec preshared key (minimum 8, maximum 256)');
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.user.limit', '8', 'The maximum number of VPN users that can be created per account');
UPDATE vm_template set unique_name='routing_old'  where id=1;
INSERT INTO vm_template (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones)
    VALUES (10, 'routing', 'SystemVM Template', 0, now(), 'ext3', 0, 64, 1, 'http://download.cloud.com/releases/2.2/systemvm.vhd.bz2', 'bcc7f290f4c27ab4d0fe95d1012829ea', 0, 'SystemVM Template', 'VHD', 15, 0, 1);
Update configuration set name='storage.max.volume.size' where name='max.volume.size.mb';
INSERT INTO sequence (name, value)
    VALUES ('snapshots_seq', '1')
UPDATE cloud.sequence SET value=IF((SELECT COUNT(*) FROM cloud.snapshots)  > 0, (SELECT max(id) FROM cloud.snapshots) + 1, 1) WHERE name='snapshots_seq'
UPDATE configuration set name='direct.attach.security.groups.enabled' where name='direct.attach.network.groups.enabled';
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (132, 2, 'Debian GNU/Linux 6(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (133, 2, 'Debian GNU/Linux 6(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (134, 3, 'Oracle Enterprise Linux 5.5 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (135, 3, 'Oracle Enterprise Linux 5.5 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (136, 4, 'Red Hat Enterprise Linux 6.0 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (137, 4, 'Red Hat Enterprise Linux 6.0 (64-bit)');
COMMIT;
