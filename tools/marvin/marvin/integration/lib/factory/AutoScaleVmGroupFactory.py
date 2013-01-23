import factory
from marvin.integration.lib.newbase import AutoScaleVmGroup
class AutoScaleVmGroupFactory(factory.Factory):

    FACTORY_FOR = AutoScaleVmGroup

    lbruleid = None
    maxmembers = None
    minmembers = None
    scaledownpolicyids = None
    scaleuppolicyids = None
    vmprofileid = None
