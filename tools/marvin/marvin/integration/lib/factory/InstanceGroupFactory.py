import factory
from marvin.integration.lib.newbase import InstanceGroup
class InstanceGroupFactory(factory.Factory):

    FACTORY_FOR = InstanceGroup

    name = None
