import factory
from marvin.integration.lib.newbase import Condition
class ConditionFactory(factory.Factory):

    FACTORY_FOR = Condition

    counterid = None
    relationaloperator = None
    threshold = None
