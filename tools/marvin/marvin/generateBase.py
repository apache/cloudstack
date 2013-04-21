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

import marvin
from marvin.cloudstackAPI import *
import os

# Add verbs in grammar - same as cloudmonkey
grammar = ['create', 'list', 'delete', 'update',
           'enable', 'activate', 'disable', 'add', 'remove',
           'attach', 'detach', 'associate', 'generate', 'ldap',
           'assign', 'authorize', 'change', 'register', 'configure',
           'start', 'restart', 'reboot', 'stop', 'reconnect',
           'cancel', 'destroy', 'revoke', 'mark', 'reset',
           'copy', 'extract', 'migrate', 'restore', 'suspend',
           'get', 'query', 'prepare', 'deploy', 'upload', 'lock',
           'disassociate', 'scale']

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
    api_classes = __import__('marvin.cloudstackAPI')

    cmdlist = map(
                    lambda f: getattr(api_classes.cloudstackAPI, f),
                    filter(
                        lambda t: t.startswith('__') == False,
                        dir(api_classes.cloudstackAPI)
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

def get_entity_from_api(api):
    matching_verbs = filter(lambda v: api.__class__.__name__.startswith(v), grammar)
    if len(matching_verbs) > 0:
        verb = matching_verbs[0]
        entity = api.__class__.__name__.replace(verb, '').replace('Cmd', '')
        return entity

def get_actionable_entities():
    cmdlets = sorted(get_api_cmds(), key=lambda k: get_entity_from_api(k))
    entities = {}
    for cmd in cmdlets:
        requireds = getattr(cmd, 'required')
        optionals = filter(lambda x: '__' not in x and 'required' not in x and 'isAsync' not in x, dir(cmd))
        matching_verbs = filter(lambda v: cmd.__class__.__name__.startswith(v), grammar)
        if len(matching_verbs)> 0:
            verb = matching_verbs[0]
            entity = cmd.__class__.__name__.replace(verb, '').replace('Cmd', '')
            if entity[:-1] in entities:
                # Accounts and Account are the same entity
                entity = entity[:-1]
            if entity[:-2] in entities:
                # IpAddresses and IpAddress are the same entity
                entity = entity[:-2]
            if entity not in entities:
                entities[entity] = { }
            entities[entity][verb] = {}
            entities[entity][verb]['args'] = requireds
            entities[entity][verb]['apimodule'] = cmd.__class__.__module__.split('.')[-1]
            entities[entity][verb]['apicmd'] = cmd.__class__.__name__
    return entities

def write_entity_classes(entities):
    tabspace = '    '
    entitydict = {}
    #TODO: Add license header for ASLv2
    for entity, actions in entities.iteritems():
        body = []
        imports = []
        imports.append('from marvin.integration.lib.base import CloudStackEntity')
        body.append('class %s(CloudStackEntity.CloudStackEntity):'%entity)
        body.append('\n')
        body.append(tabspace + 'def __init__(self, items):')
        body.append(tabspace*2 + 'self.__dict__.update(items)')
        body.append('\n')
        for action, details in actions.iteritems():
            imports.append('from marvin.cloudstackAPI import %s'%details['apimodule'])
            if action in ['create', 'list', 'deploy']:
                body.append(tabspace + '@classmethod')
            if action in ['create', 'deploy']:
                body.append(tabspace + 'def %s(cls, apiclient, %sFactory, **kwargs):'%(action, entity))
                body.append(tabspace*2 + 'cmd = %(module)s.%(command)s()'%{"module": details["apimodule"], "command": details["apicmd"]})
                body.append(tabspace*2 + '[setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in %sFactory.__dict__.iteritems()]'%entity)
                body.append(tabspace*2 + '[setattr(cmd, key, value) for key,value in kwargs.iteritems()]')
                body.append(tabspace*2 + '%s = apiclient.%s(cmd)'%(entity.lower(), details['apimodule']))
                body.append(tabspace*2 + 'return %s(%s.__dict__)'%(entity, entity.lower()))
            else:
                if len(details['args']) > 0:
                    body.append(tabspace + 'def %s(self, apiclient, %s, **kwargs):'%(action, ', '.join(list(set(details['args'])))))
                else:
                    body.append(tabspace + 'def %s(self, apiclient, **kwargs):'%(action))
                body.append(tabspace*2 + 'cmd = %(module)s.%(command)s()'%{"module": details["apimodule"], "command": details["apicmd"]})
                if action not in ['create', 'list', 'deploy']:
                    body.append(tabspace*2 + 'cmd.id = self.id')
                for arg in details['args']:
                    body.append(tabspace*2 + 'cmd.%s = %s'%(arg, arg))
                body.append(tabspace*2 + '[setattr(cmd, key, value) for key,value in kwargs.iteritems()]')
                body.append(tabspace*2 + '%s = apiclient.%s(cmd)'%(entity.lower(), details['apimodule']))
                if action in ['list']:
                    body.append(tabspace*2 + 'return map(lambda e: %s(e.__dict__), %s)'%(entity, entity.lower()))
                else:
                    body.append(tabspace*2 + 'return %s'%(entity.lower()))
            body.append('\n')

        imports = '\n'.join(imports)
        body = '\n'.join(body)
        code = imports + '\n\n' + body

        entitydict[entity] = code
        #write_entity_factory(entity, actions)
        with open("./base/%s.py"%entity, "w") as writer:
            writer.write(LICENSE)
            writer.write(code)


def write_entity_factory(entity, actions):
    tabspace = '    '
    #TODO: Add license header for ASLv2
    code = ''
    factory_defaults = []
    if 'create' in actions:
        factory_defaults.extend(actions['create']['args'])
    elif 'deploy' in actions:
        factory_defaults.extend(actions['deploy']['args'])
    elif 'associate' in actions:
        factory_defaults.extend(actions['associate']['args'])
    elif 'register' in actions:
        factory_defaults.extend(actions['register']['args'])
    else:
        return

    if os.path.exists("./factory/%sFactory.py"%entity):
        for arg in factory_defaults:
            code += tabspace + '%s = None\n'%arg
        with open("./factory/%sFactory.py"%entity, "r") as reader:
            rcode = reader.read()
            if rcode.find(code) > 0:
                return
        with open("./factory/%sFactory.py"%entity, "a") as writer:
            writer.write(code)
    else:
        code += 'import factory\n'
        code += 'from marvin.integration.lib.base import %s\n'%entity
        code += 'class %sFactory(factory.Factory):'%entity
        code += '\n\n'
        code += tabspace + 'FACTORY_FOR = %s.%s\n\n'%(entity,entity)
        for arg in factory_defaults:
            code += tabspace + '%s = None\n'%arg
        with open("./factory/%sFactory.py"%entity, "w") as writer:
            writer.write(LICENSE)
            writer.write(code)

if __name__=='__main__':
    entities = get_actionable_entities()
    write_entity_classes(entities)
