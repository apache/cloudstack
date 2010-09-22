--data upgrade from 21 to 22
use cloud;

START TRANSACTION;

DELETE FROM configuration where name='upgrade.url';
DELETE FROM configuration where name='router.template.id';
UPDATE vm_template set unique_name='routing_old'  where id=1;
INSERT INTO vm_template (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones)
    VALUES (10, 'routing', 'SystemVM Template', 0, now(), 'ext3', 0, 64, 1, 'http://download.cloud.com/releases/2.2/systemvm.vhd.bz2', 'bcc7f290f4c27ab4d0fe95d1012829ea', 0, 'SystemVM Template', 'VHD', 15, 0, 1);

ALTER TABLE `vm_instance` DROP COLUMN `group`;

COMMIT;
