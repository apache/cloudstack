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

from selenium import webdriver
import time
from selenium.common.exceptions import WebDriverException
from selenium.common.exceptions import NoSuchElementException

class Firefox(object):
    def __init__(self, x_pos = 0, y_pos = 0, x_size = 1024, y_size = 768, timeout = 30):
        self.browser = None
        self.browser = webdriver.Firefox()
        self.browser.set_page_load_timeout(timeout)
        self.browser.set_window_position(x_pos, y_pos)
        self.browser.set_window_size(x_size, y_size)

    def get_browser(self):
        return self.browser

    def set_url(self, url):
        if url == None or url == "":
            print "A valid url is required"
            return
        self.url = url
        self.browser.get(url)

    def quit_browser(self):
        try:
            self.browser.quit()
        except NoSuchElementException as err:
            print "Element error({0})".format(err.msg)
        except WebDriverException as err:
            print "WebDriver error({0})".format(err.msg)


if __name__ == "__main__":
    # Create a new instance of the Firefox driver
    browser = Firefox("Firefox")
    browser.set_url("http://10.88.90.84:8080/client/")
    time.sleep(3)
    browser.quit_browser()

