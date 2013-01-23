import factory
from marvin.integration.lib.newbase import VirtualRouterElement
class VirtualRouterElementFactory(factory.Factory):

    FACTORY_FOR = VirtualRouterElement

    nspid = None
