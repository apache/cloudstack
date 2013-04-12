import factory
from marvin.integration.lib.base import NetworkServiceProvider
class NetworkServiceProviderFactory(factory.Factory):

    FACTORY_FOR = NetworkServiceProvider

    name = None
    physicalnetworkid = None
