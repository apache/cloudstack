import factory
from marvin.integration.lib.newbase import StoragePool
class StoragePoolFactory(factory.Factory):

    FACTORY_FOR = StoragePool

    clusterid = None
    name = None
    podid = None
    url = None
    zoneid = None
