--;
-- Schema upgrade from 2.2.12 to 2.2.13;
--;

UPDATE networks SET guru_name='ExternalGuestNetworkGuru' WHERE guru_name='GuestNetworkGuru';
UPDATE nics SET reserver_name='ExternalGuestNetworkGuru' WHERE reserver_name='GuestNetworkGuru';
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm' WHERE name='hypervisor.list';

UPDATE configuration SET value='50000' WHERE name='vmware.additional.vnc.portrange.start';

INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (141, 1, 'Other CentOS (32-bit)');
INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (142, 1, 'Other CentOS (64-bit)');

UPDATE guest_os_hypervisor SET guest_os_name='Other Ubuntu Linux (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=59;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 10.04 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=121;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.10 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=122;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.04 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=123;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.10 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=124;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.04 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=125;
UPDATE guest_os_hypervisor SET guest_os_name='Other Ubuntu (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=100;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 10.04 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=126;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.10 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=127;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.04 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=128;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.10 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=129;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.04 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=130;

INSERT IGNORE INTO guest_os_hypervisor (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'CentOS (32-bit)', 141);
INSERT IGNORE INTO guest_os_hypervisor (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'CentOS (64-bit)', 142);
