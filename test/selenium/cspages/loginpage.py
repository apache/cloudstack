from selenium import webdriver
from selenium.common.exceptions import *
from selenium.webdriver.support.ui import WebDriverWait # available since 2.4.0
from selenium.webdriver.common.action_chains import ActionChains as action
from common import Global_Locators

from common.shared import *

class LoginPage(object):
    def __init__(self, browser):
        self.browser = browser
        self.username = ""
        self.password = ""
        self.language = ""

    @try_except_decor
    def set_username(self, username):
        self.username = username
        usernameElement = self.browser.find_element_by_css_selector(Global_Locators.login_username_css)
        usernameElement.send_keys(self.username)

    @try_except_decor
    def set_password(self, password):
        self.password = password
        passwordElement = self.browser.find_element_by_css_selector(Global_Locators.login_password_css)
        passwordElement.send_keys(self.password)
        self.pwelement = passwordElement

    @try_except_decor
    def set_language(self, language):
        self.language = language
        ele = self.browser.find_element_by_class_name('select-language')
        option = ele.find_element_by_xpath("//option[@value='en']")
        option.click()

        time.sleep(1)

    @try_except_decor
    def login(self, expect_fail = False):
        if self.username == "" or self.password == "":
            print "Must set email and password before logging in"
            return
        loginElement = self.browser.find_element_by_css_selector(Global_Locators.login_submit_css)
        loginElement.click()

    @try_except_decor
    def logout(self):
        # must click this icon options first
        ele1 = self.browser.find_element_by_xpath("//div[@id='user' and @class='button']/div[@class='icon options']").click()
        ele2 = self.browser.find_element_by_xpath("//div[@id='header']/div[@id='user-options']")
        ele3 = ele2.find_element_by_link_text('Logout')
        ele3.click()

        Shared.wait_for_element(self.browser, 'class_name', 'login')

    @try_except_decor
    def get_error_msg(self, loginpage_url):
        if loginpage_url is not None and len(loginpage_url) > 0 and \
            (self.browser.current_url.find(loginpage_url) > -1 or loginpage_url.find(self.browser.current_url) > -1):
            ele = self.browser.find_element_by_id('std-err')
            return ele.text
        else:
            return ""
