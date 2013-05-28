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
import inflect
grammar = ['create', 'list', 'delete', 'update', 'ldap', 'login', 'logout',
           'enable', 'activate', 'disable', 'add', 'remove',
           'attach', 'detach', 'associate', 'generate', 'assign',
           'authorize', 'change', 'register', 'configure',
           'start', 'restart', 'reboot', 'stop', 'reconnect',
           'cancel', 'destroy', 'revoke', 'mark', 'reset',
           'copy', 'extract', 'migrate', 'restore', 'suspend',
           'get', 'query', 'prepare', 'deploy', 'upload', 'lock',
           'disassociate', 'scale', 'dedicate', 'archive', 'find',
           'recover', 'release', 'resize', 'revert', 'replace']

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

def get_api_cmds(path=None):
    """ Returns the API cmdlet instances
    @param path: path where the api modules are found. defaults to pythonpath
    @return: instances of all the API commands exposed by CloudStack
    """
    if path:
        api_classes = __import__('cloudstackAPI', fromlist=['*'], level=2)
    else:
        api_classes = __import__('marvin.cloudstackAPI')

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


def singularize(word, num=0):
    """Use the inflect engine to make singular nouns of the entities
    @return: singular of `word`
    """
    inflector = inflect.engine()
    return inflector.singular_noun(word)


def transform_api(api):
    """Brute-force transform for entities that don't match other transform rules
    """
    if api == 'ldapConfig':
        return 'configure', 'Ldap'
    elif api == 'ldapRemove':
        return 'remove', 'Ldap'
    elif api == 'login':
        return 'login', 'CloudStack'
    elif api == 'logout':
        return 'logout', 'CloudStack'
    return api, None

def prefix_transformer(api, entity):
    """
    Considers the prefix as the verb when no prepositions have been found in the API
    Only if the entity is contained in the API string
    """
    if api.find(entity) > 0:
        return api[:api.find(entity)], api[api.find(entity):]
    else:
        return api, None


def post_transform_adjust(entity):
    """Some API entities are incorrectly managed

    #BUG: Inflect engine returns IpAddress => IpAddres as singular
    """
    if entity == 'IpAddres' or entity == 'IPAddres':
        return 'IpAddress'
    elif entity == 'SecurityGroupIngres':
        return 'SecurityGroupIngress'
    elif entity == 'SecurityGroupEgres':
        return 'SecurityGroupEgress'
    #CloudStack denotes VirtualMachine as UserVm
    elif entity == 'UserVm':
        return 'VirtualMachine'
    #CloudStak denotes LoadBalancer as ApplicationLoadBalancer
    elif entity == 'ApplicationLoadBalancer':
        return 'LoadBalancer'
    elif entity == 'GuestO':
        return 'GuestOS'
    elif entity == 'LBStickines':
        return 'LBStickiness'
    return entity


def prepositon_transformer(preposition=None):
    """Returns a transformer for the entity if it has a doXPrepositionY style API

    @param entity: The entity eg: resetPasswordForVirtualMachine, preposition=For
    @return: transformed entity, Y is the entity and doX is the verb, eg: VirtualMachine, resetPassword
    """
    def transform_api_with_preposition(api):
        if api.find(preposition) > 0:
            if api[api.find(preposition) + len(preposition)].isupper():
                return api[:api.find(preposition)], api[api.find(preposition) + len(preposition):]
        return api, None
    return transform_api_with_preposition


def skip_list():
    """APIs that we will not auto-generate
    """
    return []


def get_transformers():
    """ List of transform rules as lambdas
    """
    transformers = [prepositon_transformer('Of'),
                    prepositon_transformer('For'),
                    prepositon_transformer('To'),
                    prepositon_transformer('From'),
                    prepositon_transformer('With'),
                    transform_api]
    return transformers


def get_verb_and_entity(cmd):
    """Break down the API cmd instance in to `verb` and `Entity`
    @return: verb, Entity tuple
    """
    api = cmd.__class__.__name__
    api = api.replace('Cmd', '')
    #apply known list of transformations
    matching_verbs = filter(lambda v: api.startswith(v), grammar)
    if len(matching_verbs) > 0:
        for transformer in get_transformers():
            if transformer(api)[1]:
                verb = transformer(api)[0]
                entity = singularize(cmd.entity) if singularize(cmd.entity) else cmd.entity
                break
        else:
            verb = matching_verbs[0]
            entity = singularize(cmd.entity) if singularize(cmd.entity) else cmd.entity
            # In case the entity is not found/related to anything in the API
            # use the entire API as the verb.
            # eg: createPortForwardingRule => (create, FirewallRule)
            if api.lower().find(entity.lower()) < 0:
                verb = api
            else:
                verb, entity = prefix_transformer(api, entity)
        print "%s => (verb, entity) = (%s, %s)" % (api, verb, post_transform_adjust(entity))
        return verb, post_transform_adjust(entity)
    else:
        print "No matching verb, entity breakdown for api %s" % api


def get_actionable_entities(path=None):
    """
    Inspect all entities and return a map of the Entity against the actions
    along with the required arguments to satisfy the action
    @param path: path where the api modules are found. defaults to pythonpath
    @return: Dictionary of Entity { "verb" : [required] }
    """
    cmdlets = sorted(filter(lambda api: api.__class__.__name__ not in skip_list(), get_api_cmds(path)),
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
    print "Transformed %s APIs to %s entities successfully" % (len(cmdlets), len(entities)) \
            if len(cmdlets) > 0 \
            else "No transformations occurred"
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
                no_id_args = filter(lambda arg: arg!= 'id', details['args'])
                if len(no_id_args) > 0:
                    body.append(tabspace + 'def %s(self, apiclient, %s, **kwargs):' % (
                        action, ', '.join(list(set(no_id_args)))))
                else:
                    body.append(tabspace + 'def %s(self, apiclient, **kwargs):' % (action))
                body.append(tabspace * 2 + 'cmd = %(module)s.%(command)s()' % {"module": details["apimodule"],
                                                                               "command": details["apicmd"]})
                if action not in ['create', 'list', 'deploy']:
                    body.append(tabspace * 2 + 'cmd.id = self.id')
                for arg in no_id_args:
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
                if len(details["args"]) > 0:
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
