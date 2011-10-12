--;
-- Schema upgrade from 2.2.12 to 2.2.13;
--;

UPDATE networks SET guru_name='ExternalGuestNetworkGuru' WHERE guru_name='GuestNetworkGuru';
UPDATE nics SET reserver_name='ExternalGuestNetworkGuru' WHERE reserver_name='GuestNetworkGuru';
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm' WHERE name='hypervisor.list';


