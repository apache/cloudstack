import factory
from marvin.integration.lib.newbase import VirtualMachine
class VirtualMachineFactory(factory.Factory):

    FACTORY_FOR = VirtualMachine

    serviceofferingid = None
    templateid = None
    zoneid = None
