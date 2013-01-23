import factory
from marvin.integration.lib.newbase import LoadBalancerRule
class LoadBalancerRuleFactory(factory.Factory):

    FACTORY_FOR = LoadBalancerRule

    algorithm = None
    name = None
    privateport = None
    publicport = None
