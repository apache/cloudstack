update configuration set value='pod' where name='network.dns.basiczone.updates';

update configuration set value='false' where name='use.user.concentrated.pod.allocation';
update configuration set value='firstfit' where name='vm.allocation.algorithm';

update configuration set value='60' where name='expunge.delay';
update configuration set value='60' where name='expunge.interval';
update configuration set value='3' where name='expunge.workers';
update configuration set value='10' where name='workers';

update configuration set value='0' where name='capacity.check.period';
update configuration set value='-1' where name='host.stats.interval';
update configuration set value='-1' where name='vm.stats.interval';
update configuration set value='-1' where name='storage.stats.interval';
update configuration set value='-1' where name='router.stats.interval';
update configuration set value='5' where name like 'vm.op.wait.interval';

update configuration set value='10.10.10.10' where name='xen.public.network.device';
update configuration set value='zcloud.simulator' where name='guest.domain.suffix';
update configuration set value='ZIM' where name='instance.name';

update configuration set value='1000' where name='direct.agent.load.size';
update configuration set value='10000' where name='default.page.size';
update configuration set value='4' where name='linkLocalIp.nums';
update configuration set value='true' where name like '%local.storage%';
update configuration set value='false' where name like '%check.pod.cidr%';

update configuration set value='100' where name like '%network.security%pool%';
update configuration set value='120' where name like 'network.securitygroups.work.cleanup.interval';
