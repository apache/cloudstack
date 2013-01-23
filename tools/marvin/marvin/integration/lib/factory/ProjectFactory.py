import factory
from marvin.integration.lib.newbase import Project
class ProjectFactory(factory.Factory):

    FACTORY_FOR = Project

    displaytext = None
    name = None
