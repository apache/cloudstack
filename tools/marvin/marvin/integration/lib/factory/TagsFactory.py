import factory
from marvin.integration.lib.newbase import Tags
class TagsFactory(factory.Factory):

    FACTORY_FOR = Tags

    resourceids = None
    resourcetype = None
    tags = None
