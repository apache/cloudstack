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
    import inflect
except ImportError:
    raise Exception("inflect installation not found. use pip install inflect to continue")
from verbs import grammar

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

def verb_adjust(api, entity):
    """
    Considers the prefix as the verb when no preposition transformers have been found in the API
    Only if the entity is contained in the API string
    """
    index = api.lower().find(entity.lower())
    if index > 0:
        return api[:index]
    else:
        return api

def entity_adjust(entity):
    """
    Some entities are managed within CloudStack where they don't bear any resemblance to the API.
    Adjust such entities to a more sensible client side entity

    #BUG: Inflect engine returns IpAddress => IpAddres as singular
    """
    if entity == 'IpAddres' or entity == 'IPAddres':
        return 'IpAddress'
    elif entity == 'SecurityGroupIngres':
        return 'SecurityGroupIngress'
    elif entity == 'SecurityGroupEgres':
        return 'SecurityGroupEgress'
    elif entity == 'GuestO':
        return 'GuestOS'
    elif entity == 'LBStickines':
        return 'LBStickiness'
    #CloudStack denotes VirtualRouter as DomainRouter
    elif entity == 'DomainRouter':
        return 'VirtualRouter'
    #CloudStack denotes VirtualMachine as UserVm
    elif entity == 'UserVm':
        return 'VirtualMachine'
    #CloudStack denotes aliased NIC (with IP) as NicSecondaryIp
    elif entity == 'NicSecondaryIp':
        return 'Nic'
    elif entity == 'Site2SiteVpnConnection':
        return 'VpnConnection'
    elif entity == 'Site2SiteVpnGateway':
        return 'VpnGateway'
    elif entity == 'Site2SiteCustomerGateway':
        return 'VpnCustomerGateway'
    #Cloudstack returns Register entity for registerUserKeys
    elif entity == 'Register':
        return 'UserKeys'
    #Cloudstack maintains Template/ISO/Volume as single Image type
    #elif entity in ['Template', 'Volume']:
    #    return 'Image'
    #extractImage returns an Extract response but is a property of Image
    elif entity == 'Extract':
        return 'Template'
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
    return ['cleanVMReservationsCmd']

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
                if cmd.entity:
                    entity = singularize(cmd.entity) if singularize(cmd.entity) else cmd.entity
                else:
                    entity = verb, \
                                singularize(transformer(api)[1]) if singularize(transformer(api)[1]) else transformer(api)[1]
                entity = entity_adjust(entity)
                break
        else:
            verb = matching_verbs[0]
            entity = api.replace(verb, '')
            if cmd.entity:
                entity = singularize(cmd.entity) if singularize(cmd.entity) else cmd.entity
            else:
                entity = singularize(entity) if singularize(entity) else entity
            entity = entity_adjust(entity)
            verb = verb_adjust(api, entity)
        #print "%s => (verb, entity) = (%s, %s)" % (api, verb, entity)
        return verb, entity
    else:
        print "No matching verb, entity breakdown for api %s" % api