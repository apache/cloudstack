import factory
from marvin.integration.lib.newbase import ServiceForSystemVm
class ServiceForSystemVmFactory(factory.Factory):

    FACTORY_FOR = ServiceForSystemVm

    id = None
    serviceofferingid = None
