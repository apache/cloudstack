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

from marvin.cloudstackAPI import *
import os
import inflect

# Grammar for CloudStack APIs
grammar = ['create', 'list', 'delete', 'update',
           'enable', 'activate', 'disable', 'add', 'remove',
           'attach', 'detach', 'associate', 'generate', 'assign',
           'authorize', 'change', 'register', 'configure',
           'start', 'restart', 'reboot', 'stop', 'reconnect',
           'cancel', 'destroy', 'revoke', 'mark', 'reset',
           'copy', 'extract', 'migrate', 'restore', 'suspend',
           'get', 'query', 'prepare', 'deploy', 'upload', 'lock',
           'disassociate', 'scale', 'dedicate', 'archive', 'find',
           'recover', 'release', 'resize', 'revert']

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
    """@return: instances of all the API commands exposed by CloudStack
    """
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


def singularize(word, num=0):
    """Use the inflect engine to make singular nouns of the entities
    @return: singular of `word`
    """
    inflector = inflect.engine()
    return inflector.singular_noun(word)


def transform_api(api):
    """Brute-force transform for entities that don't match other transform rules
    """

    if api == 'markDefaultZoneForAccount':
        #markDefaultZoneForAccount -> Account.markDefaultZone
        return 'markDefaultZone', 'Account'
    return api, None


def prepositon_transformer(preposition=None):
    """Returns a transformer for the entity if it has a doXPrepositionY style API

    @param entity: The entity eg: resetPasswordForVirtualMachine, preposition=For
    @return: transformed entity, Y is the entity and doX is the verb, eg: VirtualMachine, resetPassword
    """
    def transform_api(api):
        if api.find(preposition) > 0:
            if api[api.find(preposition) + len(preposition)].isupper():
                return api[:api.find(preposition)], api[api.find(preposition) + len(preposition):]
        return api, None
    return transform_api


def skip_list():
    """APIs that we will not auto-generate
    """
    return ['ldapConfigCmd', 'ldapRemoveCmd']


def get_transformers():
    """ List of transform rules as lambdas
    """
    transformers = [prepositon_transformer('Of'),
                    prepositon_transformer('For'),
                    prepositon_transformer('To'),
                    prepositon_transformer('From'),
                    transform_api]
    return transformers


def get_verb_and_entity(cmd):
    """Break down the API cmd instance in to `verb` and `Entity`
    @return: verb, Entity tuple
    """
    api = cmd.__class__.__name__
    #apply known list of transformations
    for transformer in get_transformers():
        api = api.replace('Cmd', '')
        if transformer(api)[0] != api:
            print "Transforming using %s, %s -> %s" % (transformer.__name__, api, transformer(api))
            return transformer(api)[0], \
                   singularize(transformer(api)[1]) if singularize(transformer(api)[1]) else transformer(api)[1]

    matching_verbs = filter(lambda v: api.startswith(v), grammar)
    if len(matching_verbs) > 0:
        verb = matching_verbs[0]
        entity = api.replace(verb, '').replace('Cmd', '')
        return verb, singularize(entity) if singularize(entity) else entity
    else:
        print "No matching verb, entity breakdown for api %s" % api


def get_actionable_entities():
    """
    Inspect all entities and return a map of the Entity against the actions
    along with the required arguments to satisfy the action
    @return: Dictionary of Entity { "verb" : [required] }
    """
    cmdlets = sorted(filter(lambda api: api.__class__.__name__ not in skip_list(), get_api_cmds()),
        key=lambda k: get_verb_and_entity(k)[1])
    entities = {}
    for cmd in cmdlets:
        requireds = getattr(cmd, 'required')
        optionals = filter(lambda x: '__' not in x and 'required' not in x and 'isAsync' not in x, dir(cmd))
        api = cmd.__class__.__name__
        if api in skip_list():
            continue
        verb, entity = get_verb_and_entity(cmd)
        if entity not in entities:
            entities[entity] = {}
        entities[entity][verb] = {}
        entities[entity][verb]['args'] = requireds
        entities[entity][verb]['apimodule'] = cmd.__class__.__module__.split('.')[-1]
        entities[entity][verb]['apicmd'] = api
    return entities


def write_entity_classes(entities, module=None):
    """
    Writes the collected entity classes into `path`
    @param entities: dictionary of entities and the verbs acting on them
    @param module: top level module to the generated classes
    @return:
    """
    tabspace = '    '
    entitydict = {}
    for entity, actions in entities.iteritems():
        body = []
        imports = []
        imports.append('from %s.CloudStackEntity import CloudStackEntity' % module)
        body.append('class %s(CloudStackEntity):' % entity)
        #TODO: Add docs for entity
        body.append('\n\n')
        body.append(tabspace + 'def __init__(self, items):')
        body.append(tabspace * 2 + 'self.__dict__.update(items)')
        body.append('\n')
        for action, details in actions.iteritems():
            imports.append('from marvin.cloudstackAPI import %s' % details['apimodule'])
            if action in ['create', 'list', 'deploy']:
                body.append(tabspace + '@classmethod')
            if action not in ['create', 'deploy']:
                if len(details['args']) > 0:
                    body.append(tabspace + 'def %s(self, apiclient, %s, **kwargs):' % (
                        action, ', '.join(list(set(details['args'])))))
                else:
                    body.append(tabspace + 'def %s(self, apiclient, **kwargs):' % (action))
                body.append(tabspace * 2 + 'cmd = %(module)s.%(command)s()' % {"module": details["apimodule"],
                                                                               "command": details["apicmd"]})
                if action not in ['create', 'list', 'deploy']:
                    body.append(tabspace * 2 + 'cmd.id = self.id')
                for arg in details['args']:
                    body.append(tabspace * 2 + 'cmd.%s = %s' % (arg, arg))
                body.append(tabspace * 2 + '[setattr(cmd, key, value) for key,value in kwargs.iteritems()]')
                body.append(tabspace * 2 + '%s = apiclient.%s(cmd)' % (entity.lower(), details['apimodule']))
                if action in ['list']:
                    body.append(
                        tabspace * 2 + 'return map(lambda e: %s(e.__dict__), %s) if %s and len(%s) > 0 else None' % (
                            entity, entity.lower(), entity.lower(), entity.lower()))
                else:
                    body.append(tabspace * 2 + 'return %s if %s else None' % (entity.lower(), entity.lower()))
            else:
                if len(details['args']) > 0:
                    body.append(tabspace + 'def %s(cls, apiclient, %s, factory=None, **kwargs):' % (
                        action, ', '.join(map(lambda arg: arg + '=None', list(set(details['args']))))))
                else:
                    body.append(tabspace + 'def %s(cls, apiclient, factory=None, **kwargs):' % action)
                    #TODO: Add docs for actions
                body.append(tabspace * 2 + 'cmd = %(module)s.%(command)s()' % {"module": details["apimodule"],
                                                                               "command": details["apicmd"]})
                body.append(tabspace * 2 + 'if factory:')
                body.append(
                    tabspace * 3 + '[setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]')
                body.append(tabspace * 2 + 'else:')
                for arg in details["args"]:
                    body.append(tabspace * 3 + "cmd.%s = %s" % (arg, arg))
                body.append(tabspace * 2 + '[setattr(cmd, key, value) for key, value in kwargs.iteritems()]')
                body.append(tabspace * 2 + '%s = apiclient.%s(cmd)' % (entity.lower(), details['apimodule']))
                body.append(
                    tabspace * 2 + 'return %s(%s.__dict__) if %s else None' % (entity, entity.lower(), entity.lower()))
            body.append('\n')

        imports = '\n'.join(imports)
        body = '\n'.join(body)
        code = imports + '\n\n' + body

        entitydict[entity] = code
        #write_entity_factory(entity, actions, path)
        if module.find('.') > 0:
            module_path = '/'.join(module.split('.'))[1:]
        else:
            module_path = '/' + module
        if not os.path.exists(".%s" % module_path):
            os.mkdir(".%s" % module_path)
        with open(".%s/%s.py" % (module_path, entity), "w") as writer:
            writer.write(LICENSE)
            writer.write(code)


def write_entity_factory(entity, actions):
    """Data factories for each entity
    """

    tabspace = '    '
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

    if os.path.exists("./factory/%sFactory.py" % entity):
        for arg in factory_defaults:
            code += tabspace + '%s = None\n' % arg
        with open("./factory/%sFactory.py" % entity, "r") as reader:
            rcode = reader.read()
            if rcode.find(code) > 0:
                return
        with open("./factory/%sFactory.py" % entity, "a") as writer:
            writer.write(code)
    else:
        code += 'import factory\n'
        code += 'from marvin.integration.lib.base import %s\n' % entity
        code += 'class %sFactory(factory.Factory):' % entity
        code += '\n\n'
        code += tabspace + 'FACTORY_FOR = %s.%s\n\n' % (entity, entity)
        for arg in factory_defaults:
            code += tabspace + '%s = None\n' % arg
        with open("./factory/%sFactory.py" % entity, "w") as writer:
            writer.write(LICENSE)
            writer.write(code)

if __name__ == '__main__':
    entities = get_actionable_entities()
    write_entity_classes(entities, 'base2')
