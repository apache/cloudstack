import factory
from marvin.base import SecondaryStorage
class SecondaryStorageFactory(factory.Factory):

    FACTORY_FOR = SecondaryStorage

    url = None
