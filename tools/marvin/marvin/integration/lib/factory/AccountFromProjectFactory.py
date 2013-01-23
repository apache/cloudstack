import factory
from marvin.integration.lib.newbase import AccountFromProject
class AccountFromProjectFactory(factory.Factory):

    FACTORY_FOR = AccountFromProject

    account = None
    projectid = None
