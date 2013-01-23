import factory
from marvin.integration.lib.newbase import Template
class TemplateFactory(factory.Factory):

    FACTORY_FOR = Template

    displaytext = None
    name = None
    ostypeid = None
