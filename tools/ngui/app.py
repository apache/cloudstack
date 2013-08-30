#Licensed to the Apache Software Foundation (ASF) under one
#or more contributor license agreements.  See the NOTICE file
#distributed with this work for additional information
#regarding copyright ownership.  The ASF licenses this file
#to you under the Apache License, Version 2.0 (the
#"License"); you may not use this file except in compliance
#with the License.  You may obtain a copy of the License at
#http://www.apache.org/licenses/LICENSE-2.0
#Unless required by applicable law or agreed to in writing,
#software distributed under the License is distributed on an
#"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#KIND, either express or implied.  See the License for the
#specific language governing permissions and limitations
#under the License.

from flask import Flask, url_for, render_template, request, json, abort, send_from_directory
from api import apicall

app = Flask(__name__)

def get_args(multidict):
    """Default type of request.args or request.json is multidict. Converts it to dict so that can be passed to make_request"""
    data = {}
    for key in multidict.keys():
        data[key] = multidict.get(key)
    return data

@app.route('/api/<command>', methods=['GET'])
def rawapi(command):
    if request.method == 'GET':
        return apicall(command, get_args(request.args))

@app.route('/')
def index():
    return send_from_directory("templates", "index.html")

if __name__ == '__main__':
    app.run(debug=True)
