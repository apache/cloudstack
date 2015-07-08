#!/usr/bin/env python
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

from flask import abort, Flask, request, Response
from elasticsearch import Elasticsearch
import json
import time

def json_response(response):
    return json.dumps(response, indent=2) + "\n", 200, {'Content-Type': 'application/json; charset=utf-8'}

def generate_app(config=None):
    app = Flask(__name__)

    @app.route('/report/<unique_id>', methods=['POST'])
    def report(unique_id):
        # We expect JSON data, so if the Content-Type doesn't match JSON data we throw an error
        if 'Content-Type' in request.headers:
            if request.headers['Content-Type'] != 'application/json':
                abort(417, "No or incorrect Content-Type header was supplied")

        index = "cloudstack-%s" % time.strftime("%Y.%m.%d", time.gmtime())
        timestamp = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())

        es = Elasticsearch()
        es.indices.create(index=index, ignore=400)

        report = json.loads(request.data)
        report["unique_id"] = unique_id
        report["timestamp"] = timestamp

        es.index(index=index, doc_type="usage-report", body=json.dumps(report), timestamp=timestamp, refresh=True)

        response = {}
        return json_response(response)

    return app


app = generate_app()

# Only run the App if this script is invoked from a Shell
if __name__ == '__main__':
    app.debug = True
    app.run(host='0.0.0.0', port=8088)

# Otherwise provide a variable called 'application' for mod_wsgi
else:
    application = app
