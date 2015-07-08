# CloudStack Usage Report

This directory contains the CloudStack reporter webservice used by the Apache CloudStack project
to gather anonymous statistical information about CloudStack deployments.

Since version <FIX ME!!> the management server sends out a anonymized Usage Report out to the
project every 7 days.

This information is used to gain information about how CloudStack is being used.

Turning this Usage Reporting functionality off can be done in the Global Settings by setting
'usage.report.interval' to 0.

# The webservice
The Python Flask application in this directory is the webservice running on https://reports.cloudstack.apache.org/ (FIX ME?)
and stores all the incoming information in a ElasticSearch database.

Since Apache CloudStack is Open Source we show not only how we generate the report, but also how we process it.
