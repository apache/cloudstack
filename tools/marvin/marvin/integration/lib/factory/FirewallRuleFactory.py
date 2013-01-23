import factory
from marvin.integration.lib.newbase import FirewallRule
class FirewallRuleFactory(factory.Factory):

    FACTORY_FOR = FirewallRule

    ipaddressid = None
    protocol = None
