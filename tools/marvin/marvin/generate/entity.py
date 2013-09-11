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


class Entity(object):
    """Defines a cloudstack entity and associated actions
    """
    def __init__(self):
        self.name = None
        self.classname = None
        self.imports = []
        self.methods = []
        self.creator = None
        self.enumerator = None
        self.lines = []
        self.tabspace = '    '
        self.methods.append(self.update_method())

    def __str__(self):
        w = '\n'.join(self.imports)
        w = w + '\n\n'
        w = w + self.classname
        w = w + '\n\n'
        for m in self.methods:
            if m.is_enumerator():
                w = w + self.tabspace + '@classmethod'
                w = w + '\n'
            w = w + self.tabspace + m.signature
            w = w + '\n'
            for line in m.body:
                w = w + self.tabspace + line
                w = w + '\n'
            w = w + '\n'
        return w

    def update_method(self):
        """Defines the update builtin method on cloudstack entity
        """
        update = Method('__update__')
        update.signature = 'def __update__(self, items):'
        update.body = [self.tabspace + 'for key, value in items.iteritems():']
        update.body.append(self.tabspace * 2 + 'setattr(self, key, value)')
        update.body.append(self.tabspace + 'return self')
        return  update

    def generate_entity(self, entity, actions):
        self.imports.append('from cloudstackentity import CloudStackEntity')
        self.name = entity
        self.classname = 'class %s(CloudStackEntity):' % entity
        for action, details in actions.iteritems():
            self.imports.append('from marvin.cloudstackAPI import %s' % details['apimodule'])
            m = Method(action)
            self.methods.append(m)
            #TODO: doc to explain what possible args go into **kwargs
            m.docstring = 'Placeholder for docstring\n' + 'optional arguments (**kwargs): [%s]"""' % ', '.join(
                details['optionals'])
            if not m.is_creator():
                # remove the id arg as id is the self (object) itself
                no_id_args = filter(lambda arg: arg != 'id', details['args'])
                if len(no_id_args) > 0: # at least one required non-id argument
                    m.signature = 'def %s(self, apiclient, %s, **kwargs):'\
                    % (action, ', '.join(list(set(no_id_args))))
                else:
                    m.signature = 'def %s(self, apiclient, **kwargs):' % (action)
                m.body.append(self.tabspace + 'cmd = %(module)s.%(command)s()' % {"module": details["apimodule"],
                                                                                 "command": details["apicmd"]})
                if 'id' in details['args']:
                    m.body.append(self.tabspace + 'cmd.id = self.id')
                for arg in no_id_args:
                    m.body.append(self.tabspace + 'cmd.%s = %s' % (arg, arg))
                m.body.append(self.tabspace + '[setattr(cmd, key, value) for key, value in kwargs.iteritems()]')
                m.body.append(self.tabspace + '%s = apiclient.%s(cmd)' % (entity.lower(), details['apimodule']))
                if m.is_enumerator():
                    m.body.append(self.tabspace +
                                  'return map(lambda e: %s().__update__(e.__dict__), %s) '
                                  'if %s and len(%s) > 0 else None' % (
                                  entity, entity.lower(), entity.lower(), entity.lower()))
                else:
                    m.body.append(self.tabspace + 'return %s if %s else None' % (entity.lower(), entity.lower()))
            else:
                if len(details['args']) > 0: #has required arguments
                    m.signature = 'def __init__(self, apiclient=None, %s, factory=None, **kwargs):' % (
                    ', '.join(map(lambda arg: arg + '=None', list(set(details['args'])))))
                else:
                    m.signature = 'def %s(cls, apiclient, factory=None, **kwargs):' % action

                m.body.append(self.tabspace + 'self.__update__(kwargs)')
                m.body.append(self.tabspace + 'if not apiclient:')
                m.body.append(self.tabspace*2 + 'return')
                m.body.append(self.tabspace + 'self.apiclient = apiclient')

                m.body.append(self.tabspace + 'cmd = %(module)s.%(command)s()' % {"module": details["apimodule"],
                                                                               "command": details["apicmd"]})
                m.body.append(self.tabspace + 'if factory:')
                m.body.append(
                    self.tabspace * 2 + '[setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in factory.__dict__.iteritems()]')
                if len(details['args']) > 0: #has required arguments
                    m.body.append(self.tabspace + 'else:')
                    for arg in details['args']:
                        m.body.append(self.tabspace * 2 + "cmd.%s = %s" % (arg, arg))
                m.body.append(self.tabspace + '[setattr(cmd, key, value) for key, value in kwargs.iteritems()]')
                m.body.append(self.tabspace + '%s = apiclient.%s(cmd)' % (entity.lower(), details['apimodule']))
                m.body.append(
                    self.tabspace + 'self.__update__(%s.__dict__) if %s else None' % (entity.lower(), entity.lower()))

class Method(object):
    """A method object defining action on an entity

    - contains attributes, signature, docstring and body
    """
    def __init__(self, action):
        self.action = action
        self.docstring = None
        self.signature = None
        self.body = []

    def is_creator(self):
        """ Any action that results in the creation of the entity is an entity creator

        eg: createNetwork, deployVirtualMachine or registerIso
        @param action: action verb
        @return: True if creator False otherwise
        """
        if self.action.startswith('create') \
               or self.action.startswith('register') \
               or self.action.startswith('deploy'):
            return True
        return False

    def is_enumerator(self):
        """ Any action that lists existing entities is an entity enumerator

        eg: listXxx APIs
        @param action: action verb
        @return: True if enumerator False otherwise
        """
        if self.action.startswith('list'):
            return True
        return False


class Creator(Method):
    """A creator method - typically one that creates the entity
    """
    def __init__(self):
        self.decorators = ["@classmethod"]


class Enumerator(Method):
    """An enumerator method  - typically one that lists entities
    """
    def __init__(self):
        self.decorators = ["@classmethod"]
