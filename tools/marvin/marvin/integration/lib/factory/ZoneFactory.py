import factory
from marvin.integration.lib.newbase import Zone
class ZoneFactory(factory.Factory):

    FACTORY_FOR = Zone

    dns1 = None
    internaldns1 = None
    name = None
    networktype = None
