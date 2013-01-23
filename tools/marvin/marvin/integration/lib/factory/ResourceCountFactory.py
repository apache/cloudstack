import factory
from marvin.integration.lib.newbase import ResourceCount
class ResourceCountFactory(factory.Factory):

    FACTORY_FOR = ResourceCount

    domainid = None
