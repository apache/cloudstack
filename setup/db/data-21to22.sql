--data upgrade from 21 to 22
use cloud;

START TRANSACTION;

DELETE FROM configuration where name='upgrade.url';

COMMIT;
