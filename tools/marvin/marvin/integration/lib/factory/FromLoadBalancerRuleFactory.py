import factory
from marvin.integration.lib.newbase import FromLoadBalancerRule
class FromLoadBalancerRuleFactory(factory.Factory):

    FACTORY_FOR = FromLoadBalancerRule

    id = None
    virtualmachineids = None
