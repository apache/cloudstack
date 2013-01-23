import factory
from marvin.integration.lib.newbase import PortForwardingRule
class PortForwardingRuleFactory(factory.Factory):

    FACTORY_FOR = PortForwardingRule

    ipaddressid = None
    privateport = None
    protocol = None
    publicport = None
    virtualmachineid = None
