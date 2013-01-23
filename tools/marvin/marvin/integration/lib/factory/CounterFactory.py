import factory
from marvin.integration.lib.newbase import Counter
class CounterFactory(factory.Factory):

    FACTORY_FOR = Counter

    name = None
    source = None
    value = None
