alter table disk_offering add bytes_read_rate_max bigint(20) default null after bytes_read_rate;
alter table disk_offering add bytes_read_rate_max_length bigint(20) default null after bytes_read_rate_max;
alter table disk_offering add bytes_write_rate_max bigint(20) default null after bytes_write_rate;
alter table disk_offering add bytes_write_rate_max_length bigint(20) default null after bytes_write_rate_max;
alter table disk_offering add iops_read_rate_max bigint(20) default null after iops_read_rate;
alter table disk_offering add iops_read_rate_max_length bigint(20) default null after iops_read_rate_max;
alter table disk_offering add iops_write_rate_max bigint(20) default null after iops_write_rate;
alter table disk_offering add iops_write_rate_max_length bigint(20) default null after iops_write_rate_max;
