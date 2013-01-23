import factory
from marvin.integration.lib.newbase import SecurityGroup
class SecurityGroupFactory(factory.Factory):

    FACTORY_FOR = SecurityGroup

    name = None
