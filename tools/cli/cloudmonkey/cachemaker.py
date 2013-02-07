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
    import types

    from config import cache_file
except ImportError, e:
    import sys
    print "ImportError", e
    sys.exit(1)


def getvalue(dictionary, key):
    if key in dictionary:
        return dictionary[key]
    else:
        return None


def splitcsvstring(string):
    if string is not None:
        return filter(lambda x: x.strip() != '', string.split(','))
    else:
        return []


def splitverbsubject(string):
    idx = 0
    for char in string:
        if char.islower():
            idx += 1
        else:
            break
    return string[:idx].lower(), string[idx:].lower()


def savecache(apicache, json_file):
    """
    Saves apicache dictionary as json_file, returns dictionary as indented str
    """
    if apicache is None or apicache is {}:
        return ""
    apicachestr = json.dumps(apicache, indent=2)
    with open(json_file, 'w') as cache_file:
        cache_file.write(apicachestr)
    return apicachestr


def loadcache(json_file):
    """
    Loads json file as dictionary, feeds it to monkeycache and spits result
    """
    f = open(json_file, 'r')
    data = f.read()
    f.close()
    try:
        apicache = json.loads(data)
    except ValueError, e:
        print "Error processing json:", json_file, e
        return {}
    return apicache


def monkeycache(apis):
    """
    Feed this a dictionary of api bananas, it spits out processed cache
    """
    if isinstance(type(apis), types.NoneType) or apis is None:
        return {}

    responsekey = filter(lambda x: 'response' in x, apis.keys())

    if len(responsekey) == 0:
        print "[monkeycache] Invalid dictionary, has no response"
        return None
    if len(responsekey) != 1:
        print "[monkeycache] Multiple responsekeys, chosing first one"

    responsekey = responsekey[0]
    verbs = set()
    cache = {}
    cache['count'] = getvalue(apis[responsekey], 'count')
    cache['asyncapis'] = []

    for api in getvalue(apis[responsekey], 'api'):
        name = getvalue(api, 'name')
        verb, subject = splitverbsubject(name)

        apidict = {}
        apidict['name'] = name
        apidict['description'] = getvalue(api, 'description')
        apidict['isasync'] = getvalue(api, 'isasync')
        if apidict['isasync']:
            cache['asyncapis'].append(name)
        apidict['related'] = splitcsvstring(getvalue(api, 'related'))

        required = []
        apiparams = []
        for param in getvalue(api, 'params'):
            apiparam = {}
            apiparam['name'] = getvalue(param, 'name')
            apiparam['description'] = getvalue(param, 'description')
            apiparam['required'] = (getvalue(param, 'required') is True)
            apiparam['length'] = int(getvalue(param, 'length'))
            apiparam['type'] = getvalue(param, 'type')
            apiparam['related'] = splitcsvstring(getvalue(param, 'related'))
            if apiparam['required']:
                required.append(apiparam['name'])
            apiparams.append(apiparam)

        apidict['requiredparams'] = required
        apidict['params'] = apiparams
        if verb not in cache:
            cache[verb] = {}
        cache[verb][subject] = apidict
        verbs.add(verb)

    cache['verbs'] = list(verbs)
    return cache


def main(json_file):
    """
    cachemaker.py creates a precache datastore of all available apis of
    CloudStack and dumps the precache dictionary in an
    importable python module. This way we cheat on the runtime overhead of
    completing commands and help docs. This reduces the overall search and
    cache_miss (computation) complexity from O(n) to O(1) for any valid cmd.
    """
    f = open("precache.py", "w")
    f.write("""# -*- coding: utf-8 -*-
# Auto-generated code by cachemaker.py
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
    f.write("\napicache = %s" % loadcache(json_file))
    f.close()

if __name__ == "__main__":
    print "[cachemaker] Pre-caching using user's cloudmonkey cache", cache_file
    if os.path.exists(cache_file):
        main(cache_file)
    else:
        print "[cachemaker] Unable to cache apis, file not found", cache_file
        print "[cachemaker] Run cloudmonkey sync to generate cache"
