import factory
from marvin.integration.lib.newbase import ServiceOffering
class ServiceOfferingFactory(factory.Factory):

    FACTORY_FOR = ServiceOffering

    cpunumber = None
    cpuspeed = None
    displaytext = None
    memory = None
    name = None
