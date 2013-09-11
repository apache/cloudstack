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

class Factory(object):
    """Defines a cloudstack factory object
    """
    def __init__(self):
        self.name = None
        self.code = None

    def __str__(self):
        return self.code

    def generate_factory(self, entity, actions):
        """Data factories for each entity
        """

        tabspace = '    '
        code = ''
        factory_defaults = []
        keys = actions.keys()
        for key in keys:
            if key.startswith('create'):
                factory_defaults.extend(actions[key]['args'])
            elif key.startswith('deploy'):
                factory_defaults.extend(actions[key]['args'])
            elif key.startswith('associate'):
                factory_defaults.extend(actions[key]['args'])
            elif key.startswith('register'):
                factory_defaults.extend(actions[key]['args'])
            else:
                continue
                #print '%s is not suitable for factory creation for entity %s' %(key, entity)

        factory_defaults = set(factory_defaults)
        code += 'from marvin.entity.%s import %s\n' % (entity.lower(), entity)
        code += 'from factory import Factory'
        code += '\n\n'
        code += 'class %sFactory(Factory):' % entity
        code += '\n\n'
        code += tabspace + 'FACTORY_FOR = %s\n' % entity
        code += tabspace + 'FACTORY_ARG_PARAMETERS = (\'apiclient\',)\n\n'
        code += tabspace + 'apiclient = None\n'
        for arg in factory_defaults:
            code += tabspace + '%s = None\n' % arg
        self.name = entity
        self.code = code