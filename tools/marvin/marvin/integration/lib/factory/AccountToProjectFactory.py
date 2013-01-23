import factory
from marvin.integration.lib.newbase import AccountToProject
class AccountToProjectFactory(factory.Factory):

    FACTORY_FOR = AccountToProject

    projectid = None
