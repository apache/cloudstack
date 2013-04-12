import factory
from marvin.integration.lib.base import StaticNat
class StaticNatFactory(factory.Factory):

    FACTORY_FOR = StaticNat

    ipaddressid = None
    virtualmachineid = None
