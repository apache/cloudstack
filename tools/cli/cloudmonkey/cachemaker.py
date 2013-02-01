# -*- coding: utf-8 -*-
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

try:
    import json
    import os
    import re

    from requester import monkeyrequest
except ImportError, e:
    import sys
    print "ImportError", e
    sys.exit(1)


def getvalue(dictionary, key):
    if key in dictionary:
        return dictionary[key]
    else:
        return None


def csv_str_as_list(string):
    if string is not None:
        return filter(lambda x: x.strip() != '', string.split(','))
    else:
        return []


def cachegen_from_file(json_file):
    f = open(json_file, 'r')
    data = f.read()
    f.close()
    try:
        apis = json.loads(data)
    except ValueError, e:
        print "Error processing json in cachegen()", e
    return cachegen(apis)


def cachegen(apis):
    pattern = re.compile("[A-Z]")
    responsekey = filter(lambda x: 'response' in x, apis.keys())

    if len(responsekey) == 0:
        print "[cachegen] Invalid dictionary, has no response"
        return None
    if len(responsekey) != 1:
        print "[cachegen] Multiple responsekeys, chosing first one"

    responsekey = responsekey[0]
    verbs = set()
    cache = {}
    cache['count'] = getvalue(apis[responsekey], 'count')

    for api in getvalue(apis[responsekey], 'api'):
        name = getvalue(api, 'name')
        response = getvalue(api, 'response')

        idx = pattern.search(name).start()
        verb = name[:idx]
        subject = name[idx:]

        apidict = {}
        apidict['name'] = name
        apidict['description'] = getvalue(api, 'description')
        apidict['isasync'] = getvalue(api, 'isasync')
        apidict['related'] = csv_str_as_list(getvalue(api, 'related'))

        required = []
        apiparams = []
        for param in getvalue(api, 'params'):
            apiparam = {}
            apiparam['name'] = getvalue(param, 'name')
            apiparam['description'] = getvalue(param, 'description')
            apiparam['required'] = (getvalue(param, 'required') is True)
            apiparam['length'] = int(getvalue(param, 'length'))
            apiparam['type'] = getvalue(param, 'type')
            apiparam['related'] = csv_str_as_list(getvalue(param, 'related'))
            if apiparam['required']:
                required.append(apiparam['name'])
            apiparams.append(apiparam)

        apidict['requiredparams'] = required
        apidict['params'] = apiparams
        apidict['response'] = getvalue(api, 'response')
        cache[verb] = {subject: apidict}
        verbs.add(verb)

    cache['verbs'] = list(verbs)
    return cache


def main(json_file):
    """
    cachegen.py creates a precache datastore of all available apis of
    CloudStack and dumps the precache dictionary in an
    importable python module. This way we cheat on the runtime overhead of
    completing commands and help docs. This reduces the overall search and
    cache_miss (computation) complexity from O(n) to O(1) for any valid cmd.
    """
    f = open("precache.py", "w")
    f.write("""# -*- coding: utf-8 -*-
# Auto-generated code by cachegen.py
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
# under the License.""")
    f.write("\nprecache = %s" % cachegen_from_file(json_file))
    f.close()

if __name__ == "__main__":
    json_file = 'listapis.json'
    if os.path.exists(json_file):
        main(json_file)
    else:
        pass
        #print "[ERROR] cli:cachegen is unable to locate %s" % json_file
