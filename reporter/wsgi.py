# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# WSGI entry point for mod_wsgi, gunicorn, uWSGI, etc.
# The main application file uses a hyphenated name which cannot be imported
# directly, so this shim loads it via importlib.
#
# mod_wsgi (Apache):
#   WSGIScriptAlias /report /path/to/reporter/wsgi.py
#
# gunicorn:
#   gunicorn wsgi:application
#
# uWSGI:
#   uwsgi --wsgi-file wsgi.py --callable application

import importlib.util
import os

_spec = importlib.util.spec_from_file_location(
    "usage_report_collector",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "usage-report-collector.py")
)
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)

application = _mod.app
