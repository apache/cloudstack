import factory
from marvin.integration.lib.newbase import ServiceForVirtualMachine
class ServiceForVirtualMachineFactory(factory.Factory):

    FACTORY_FOR = ServiceForVirtualMachine

    id = None
    serviceofferingid = None
