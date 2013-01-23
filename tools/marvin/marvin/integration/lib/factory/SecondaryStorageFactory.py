import factory
from marvin.integration.lib.newbase import SecondaryStorage
class SecondaryStorageFactory(factory.Factory):

    FACTORY_FOR = SecondaryStorage

    url = None
