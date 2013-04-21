import factory
from marvin.base import StaticNat
class StaticNatFactory(factory.Factory):

    FACTORY_FOR = StaticNat

    ipaddressid = None
    virtualmachineid = None
