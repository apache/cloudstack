import factory
from marvin.integration.lib.newbase import VlanIpRange
class VlanIpRangeFactory(factory.Factory):

    FACTORY_FOR = VlanIpRange

    startip = None
