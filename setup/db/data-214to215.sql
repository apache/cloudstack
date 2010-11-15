use cloud;

-- In case there are more DML's to be added please wrap it around transaction. Refer data-20to21 script
--
-- Bug# 6748. This query deletes all the previous stats for secondary storage. On successful migration new row/rows would be created reflecting the correct stats for secondary storage 
delete  FROM op_host_capacity where capacity_type=6;