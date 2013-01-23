import factory
from marvin.integration.lib.newbase import StorageNetworkIpRange
class StorageNetworkIpRangeFactory(factory.Factory):

    FACTORY_FOR = StorageNetworkIpRange

    gateway = None
    netmask = None
    podid = None
    startip = None
