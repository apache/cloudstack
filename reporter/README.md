<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# CloudStack Usage Reporter

This directory contains the server-side webservice for the Apache CloudStack usage reporting feature. When enabled, CloudStack management servers periodically send an anonymized report to the Apache CloudStack project. This data helps the community understand how CloudStack is deployed and used in the field.

All data collected is anonymous. No personally identifiable information, IP addresses, or workload data is transmitted.

## Enabling usage reporting

Usage reporting is configured through CloudStack's Global Settings. Two settings are available:

| Setting | Default | Description |
|---|---|---|
| `usage.report.interval` | `0` (disabled) | Interval in days between reports. Set to `7` to enable weekly reporting. Changing this setting requires a restart of the Management Server. |
| `usage.report.uri` | `https://reporting.cloudstack.org/report` | The endpoint reports are sent to. Only HTTPS is supported. |

## The webservice

The collector is a Python Flask application (`usage-report-collector.py`) that receives reports and stores them as JSON files on the local filesystem. It exposes a single endpoint:

```
POST /report/<unique_id>
```

The `unique_id` is a SHA-256 hash derived from the management server's database, ensuring reports from the same installation can be correlated across time without identifying the operator.

### Storage

Reports are stored below a base directory, configurable through the `REPORT_DIR` environment variable (default: `reports` in the working directory). A directory is created per `unique_id` and each report is stored with its receive timestamp as the filename:

```
reports/
  <unique_id>/
    2026-08-07T09-15-04Z.json
    2026-08-14T09-15-11Z.json
```

### Validation

To keep malicious or malformed submissions out, the collector rejects reports that are not JSON objects, exceed 1MB, nest deeper than 6 levels, contain more than 4096 keys, or contain non-printable or oversized keys and string values. Only string, number and boolean values are accepted. The `unique_id` must be a valid SHA-256 hex digest. Per `unique_id`, at most one report per hour is accepted and at most 1000 reports are kept — the oldest are removed first, so a single sender can never fill up the disk.

### Running the webservice

Install dependencies:

```bash
pip install -r requirements.txt
```

**Development:**

```bash
python usage-report-collector.py
```

**Production (gunicorn):**

```bash
gunicorn wsgi:application
```

**Production (uWSGI):**

```bash
uwsgi --wsgi-file wsgi.py --callable application
```

**Production (Apache mod_wsgi):**

```apache
WSGIScriptAlias /report /path/to/reporter/wsgi.py
```

## Open source transparency

In the spirit of open source, the Apache CloudStack project publishes both the client-side code that generates reports (see `UsageReporter.java`) and this server-side collector. You can inspect exactly what data is sent and how it is stored.