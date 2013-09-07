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

import os
from marvin.generate.entity import Entity
from marvin.generate.factory import Factory
from marvin.generate.linguist import *

LICENSE = """# Licensed to the Apache Software Foundation (ASF) under one
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
"""

def get_api_cmds():
    """ Returns the API cmdlet instances

    @return: instances of all the API commands exposed by CloudStack
    """
    namespace = {}
    execfile('cloudstackAPI/__init__.py', namespace)
    api_classes = __import__('cloudstackAPI', globals().update(namespace), fromlist=['*'], level=-1)


    cmdlist = map(
        lambda f: getattr(api_classes, f),
        filter(
            lambda t: t.startswith('__') == False,
            dir(api_classes)
        )
    )
    cmdlist = filter(
        lambda g: g is not None,
        cmdlist
    )
    clslist = map(
        lambda g: getattr(g, g.__name__.split('.')[-1] + 'Cmd'),
        filter(
            lambda h: h.__name__.split('.')[-1] not in ['baseCmd', 'baseResponse', 'cloudstackAPIClient'],
            cmdlist
        )
    )
    cmdlets = map(lambda t: t(), clslist)
    return cmdlets

def get_entity_action_map():
    """ Inspect cloudstack api and return a map of the Entity against the actions
    along with the required arguments to make the action call

    @return: Dictionary of Entity { "verb" : [required] }
    eg: VirtualMachine { "deploy" : [templateid, serviceoffering, zoneid, etc] }
    """
    cmdlets = sorted(filter(lambda api: api.__class__.__name__ not in skip_list(), get_api_cmds()),
        key=lambda k: get_verb_and_entity(k)[1])

    entities = {}
    for cmd in cmdlets:
        requireds = getattr(cmd, 'required')
        optionals = filter(lambda x: '__' not in x and x not in ['required', 'isAsync', 'entity'], dir(cmd))
        api = cmd.__class__.__name__
        if api in skip_list():
            continue
        verb, entity = get_verb_and_entity(cmd)
        if entity not in entities:
            entities[entity] = {}
        entities[entity][verb] = {}
        entities[entity][verb]['args'] = requireds
        entities[entity][verb]['optionals'] = optionals
        entities[entity][verb]['apimodule'] = cmd.__class__.__module__.split('.')[-1]
        entities[entity][verb]['apicmd'] = api
    print "Transformed %s APIs to %s entities successfully" % (len(cmdlets), len(entities)) \
            if len(cmdlets) > 0 \
            else "No transformations occurred"
    return entities

def write(entity_or_factory, module):
    module_path = './' + '/'.join(module.split('.'))
    if not os.path.exists("%s" % module_path):
        os.makedirs("%s" % module_path)
    with open("%s/__init__.py" % (module_path), "w") as writer:
        writer.write(LICENSE)
    with open("%s/%s.py" % (module_path, entity_or_factory.name.lower()), "w") as writer:
        writer.write(LICENSE)
        writer.write(entity_or_factory.__str__())

def generate(entities):
    """
    Writes the collected entity classes

    @param entities: dictionary of entities and the verbs acting on them
    @return:
    """
    for entity, actions in entities.iteritems():
        e = Entity()
        f = Factory()

        e.generate_entity(entity, actions)
        f.generate_factory(entity, actions)

        write(e, module='entity')
        write(f, module='factory')

if __name__ == '__main__':
    entities = get_entity_action_map()
    generate(entities)
