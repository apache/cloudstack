import factory
from marvin.integration.lib.newbase import StaticRoute
class StaticRouteFactory(factory.Factory):

    FACTORY_FOR = StaticRoute

    cidr = None
    gatewayid = None
