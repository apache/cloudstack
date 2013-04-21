import factory
from marvin.base import NetworkServiceProvider
class NetworkServiceProviderFactory(factory.Factory):

    FACTORY_FOR = NetworkServiceProvider

    name = None
    physicalnetworkid = None
