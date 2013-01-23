import factory
from marvin.integration.lib.newbase import LBStickinessPolicy
class LBStickinessPolicyFactory(factory.Factory):

    FACTORY_FOR = LBStickinessPolicy

    lbruleid = None
    methodname = None
    name = None
