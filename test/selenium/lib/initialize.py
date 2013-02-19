'''
This will help pass webdriver (Browser instance) across our test cases.
'''



from selenium import webdriver

DRIVER = None

def getOrCreateWebdriver():
    global DRIVER
    DRIVER = DRIVER or webdriver.Firefox()
    return DRIVER
    
