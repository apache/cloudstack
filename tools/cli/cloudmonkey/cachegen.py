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
    import re
    from marvin.cloudstackAPI import *
    from marvin import cloudstackAPI
except ImportError, e:
    import sys
    print "ImportError", e
    sys.exit(1)

completions = cloudstackAPI.__all__


def get_api_module(api_name, api_class_strs=[]):
    try:
        api_mod = __import__("marvin.cloudstackAPI.%s" % api_name,
                             globals(), locals(), api_class_strs, -1)
    except ImportError, e:
        print "Error: API not found", e
        return None
    return api_mod


def main():
    """
    cachegen.py creates a precached dictionary for all the available verbs in
    the predefined grammar of cloudmonkey, it dumps the dictionary in an
    importable python module. This way we cheat on the runtime overhead of
    completing commands and help docs. This reduces the overall search and
    cache_miss (computation) complexity from O(n) to O(1) for any valid cmd.
    """
    pattern = re.compile("[A-Z]")
    verbs = list(set([x[:pattern.search(x).start()] for x in completions
                 if pattern.search(x) is not None]).difference(['cloudstack']))
    # datastructure {'verb': {cmd': ['api', [params], doc, required=[]]}}
    cache_verbs = {}
    for verb in verbs:
        completions_found = filter(lambda x: x.startswith(verb), completions)
        cache_verbs[verb] = {}
        for api_name in completions_found:
            api_cmd_str = "%sCmd" % api_name
            api_mod = get_api_module(api_name, [api_cmd_str])
            if api_mod is None:
                continue
            try:
                api_cmd = getattr(api_mod, api_cmd_str)()
                required = api_cmd.required
                doc = api_mod.__doc__
            except AttributeError, e:
                print "Error: API attribute %s not found!" % e
            params = filter(lambda x: '__' not in x and 'required' not in x,
                            dir(api_cmd))
            if len(required) > 0:
                doc += "\nRequired args: %s" % " ".join(required)
            doc += "\nArgs: %s" % " ".join(params)
            api_name_lower = api_name.replace(verb, '').lower()
            cache_verbs[verb][api_name_lower] = [api_name, params, doc,
                                                 required]
    f = open("precache.py", "w")
    f.write("""# Auto-generated code by cachegen.py
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
    f.write("\nprecached_verbs = %s" % cache_verbs)
    f.close()

if __name__ == "__main__":
    main()
