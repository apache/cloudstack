import factory
from marvin.integration.lib.newbase import Iso
class IsoFactory(factory.Factory):

    FACTORY_FOR = Iso

    displaytext = None
    name = None
    url = None
    zoneid = None
