import factory
from marvin.integration.lib.newbase import ToLoadBalancerRule
class ToLoadBalancerRuleFactory(factory.Factory):

    FACTORY_FOR = ToLoadBalancerRule

    id = None
    virtualmachineids = None
