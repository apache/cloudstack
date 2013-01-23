import factory
from marvin.integration.lib.newbase import AutoScalePolicy
class AutoScalePolicyFactory(factory.Factory):

    FACTORY_FOR = AutoScalePolicy

    action = None
    conditionids = None
    duration = None
