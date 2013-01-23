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
           'get', 'query', 'prepare', 'deploy', 'upload', 'lock', 'disassociate']

aslv2 = ""


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
            entities[entity][verb] = [requireds,optionals]
    return entities

def write_entity_classes(entities):
    tabspace = '    '
    classlist = []
    #TODO: Add license header for ASLv2
    code = ''
    for entity, actions in entities.iteritems():
        code += 'class %s:'%entity
        for action, args in actions.iteritems():
            code += '\n\n'
            code += tabspace
            if action.startswith('create'):
                code += 'def %s(self, apiclient, %sFactory'%(action, entity)
            else:
                code += 'def %s(self, apiclient'%(action)
            if len(args[0]) > 0:
                code += ', ' + ', '.join(list(set(args[0])))
            if len(args[1]) > 0:
                code += ', **kwargs):\n'
            else:
                code += '):\n'
            code += tabspace*2
            code += 'pass'
        code += '\n\n'
        classlist.append(code)
        write_entity_factory(entity, actions)
    return list(set(classlist))

def write_entity_factory(entity, actions):
    tabspace = '    '
    #TODO: Add license header for ASLv2
    code = ''
    if 'create' not in actions:
        return
    factory_defaults = actions['create']
    if os.path.exists("./factory/%sFactory.py"%entity):
        for arg in factory_defaults[0]:
            code += tabspace + '%s = None\n'%arg
        with open("./factory/%sFactory.py"%entity, "a") as writer:
            writer.write(code)
    else:
        code += 'import factory\n'
        code += 'from marvin.integration.lib.newbase import %s\n'%entity
        code += 'class %sFactory(factory.Factory):'%entity
        code += '\n\n'
        code += tabspace + 'FACTORY_FOR = %s\n\n'%entity
        for arg in factory_defaults[0]:
            code += tabspace + '%s = None\n'%arg
        with open("./factory/%sFactory.py"%entity, "w") as writer:
            writer.write(code)

if __name__=='__main__':
    entities = get_actionable_entities()
    clslist = write_entity_classes(entities)
    for cls in clslist:
        print cls
