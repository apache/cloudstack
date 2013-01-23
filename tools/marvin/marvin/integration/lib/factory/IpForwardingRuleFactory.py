import factory
from marvin.integration.lib.newbase import IpForwardingRule
class IpForwardingRuleFactory(factory.Factory):

    FACTORY_FOR = IpForwardingRule

    ipaddressid = None
    protocol = None
    startport = None
