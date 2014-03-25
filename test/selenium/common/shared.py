# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#!/usr/bin/python
# coding: latin-1

from selenium.selenium import selenium
from selenium.common.exceptions import NoSuchElementException
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.common.exceptions import WebDriverException
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.support import expected_conditions as EC # available since 2.26.0
from selenium.webdriver.support.ui import WebDriverWait # available since 2.4.0
import re, sys, time, traceback

def try_except_decor(func):
    def try_except(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except WebDriverException as err:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            print "WebDriver error. Function: {0}, error: {1}".format(func.func_code, err)
            print repr(traceback.format_exception(exc_type, exc_value,exc_traceback))
        except NoSuchElementException as err:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            print "Element error. Function: {0}, error: {1}".format(func.func_code, err)
            print repr(traceback.format_exception(exc_type, exc_value,exc_traceback))
        except TimeoutException as err:
            exc_type, exc_value, exc_traceback = sys.exc_info()
            print "Timeout error. Function: {0}, error: {1}".format(func.func_code, err)
            print repr(traceback.format_exception(exc_type, exc_value,exc_traceback))

    return try_except

class Shared(object):

    @staticmethod
    @try_except_decor
    def option_selection(browser, element_type, element_name, option_text, wait_element_type = '', wait_element_name = ''):

        ret = False
        Shared.wait_for_element(browser, element_type, element_name)
        if element_type == 'id':
            ele = browser.find_element_by_id(element_name)
        elif element_type == 'class_name':
            ele = browser.find_element_by_class_name(element_name)
        options = ele.find_elements_by_tag_name('option')
        option_names = [option.text for option in options]
        if option_text not in option_names:
            return ret

        for option in options:
            if option.text.find(option_text) > -1:
                option.click()
                ret = True
                time.sleep(1)
                break

        if len(wait_element_type) > 0 and len(wait_element_name) > 0:
            Shared.wait_for_element(browser, wait_element_type, wait_element_name)
        return ret

    @staticmethod
    @try_except_decor
    def flash_message(browser):
        try:
            ele1 = browser.find_element_by_id('flashMessageArea')
        except NoSuchElementException:
            ele1 = None
        if ele1 != None:
            ele2 = ele1.find_element_by_class_name('flash_message')
            if ele2 != None and ele2.text != None and len(ele2.text) > 0:
                return ele2.text
            else:
                return ''
        else:
            return ''

    @staticmethod
    @try_except_decor
    def string_selection(browser, key, value, index = 0):
        element = browser.find_elements_by_id(key)[index]
        element.clear()
        element.send_keys(value)

    @staticmethod
    def wait_until_title_text(browser, text, waittime = 30):
        wait = WebDriverWait(browser, waittime)
        wait.until(lambda browser: browser.title.lower().find(text.lower()) > -1)

    @staticmethod
    def wait_until_find_id(browser, element_id, waittime = 10):
        wait = WebDriverWait(browser, waittime)
        wait.until(lambda browser: browser.find_element_by_id(element_id))

    @staticmethod
    # the name should exist in the newer page, but not in older one
    def wait_for_element(browser, element_type, name, waittime = 30):
        wait = WebDriverWait(browser, waittime)
        if element_type.lower() == 'id':
            wait.until(EC.presence_of_element_located((By.ID, name)))
        elif element_type.lower() == 'tag_name':
            wait.until(EC.presence_of_element_located((By.TAG_NAME, name)))
        elif element_type.lower() == 'class_name':
            wait.until(EC.presence_of_element_located((By.CLASS_NAME, name)))
        elif element_type.lower() == 'xpath':
            wait.until(EC.presence_of_element_located((By.XPATH, name)))
        elif element_type.lower() == 'link_text':
            wait.until(EC.presence_of_element_located((By.LINK_TEXT, name)))

        #feed the string through directly
        else:
            wait.until(EC.presence_of_element_located(element_type, name))

        time.sleep(1)

    def playing_around(self):
        from threading import Timer
        t = Timer(20,self.wait_for_invisible)
        t.start()
 
    @staticmethod
    #wait until something disappears
    def wait_for_invisible(browser, element_type, name, waittime=30):
        wait = WebDriverWait(browser, waittime)

        # the code base uses underscores, but the real string doesn't have em.
        final_type = re.sub('_',' ',element_type)

        wait.until(EC.invisibility_of_element_located((final_type, name)))

        #this method isn't as slick as I hoped :(
        time.sleep(1)

