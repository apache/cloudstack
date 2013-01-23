import factory
from marvin.integration.lib.newbase import AutoScaleVmProfile
class AutoScaleVmProfileFactory(factory.Factory):

    FACTORY_FOR = AutoScaleVmProfile

    serviceofferingid = None
    templateid = None
    zoneid = None
