import factory
from marvin.integration.lib.newbase import DiskOffering
class DiskOfferingFactory(factory.Factory):

    FACTORY_FOR = DiskOffering

    displaytext = None
    name = None
