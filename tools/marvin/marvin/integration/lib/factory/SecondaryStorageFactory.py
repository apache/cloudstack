import factory
from marvin.integration.lib.base import SecondaryStorage
class SecondaryStorageFactory(factory.Factory):

    FACTORY_FOR = SecondaryStorage

    url = None
