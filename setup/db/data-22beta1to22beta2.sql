INSERT INTO sequence (name, value)
    VALUES ('snapshots_seq', '1')
UPDATE cloud.sequence SET value=IF((SELECT COUNT(*) FROM cloud.snapshots)  > 0, (SELECT max(id) FROM cloud.snapshots) + 1, 1) WHERE name='snapshots_seq'
