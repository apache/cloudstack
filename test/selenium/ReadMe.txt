##############################################

Questions? Post'em @ dev@cloudstack.apache.org

##############################################

This files contains following:

1) Installation requirements
2) Testing pre-requisites
3) Running the Tests and Generating the report
##############################################



##########################################################################################################################################

1) Installation Requirements
---------------------------


1) Firefox depending on your OS (Good to have Firebug and Selenium IDE for troubleshooting and dev work)


2) Install Python 2.7.


3) Now Open CMD/Terminal and type all of following

- pip install pycrypto (Installs Pycrypto)
- pip install paramiko (Install paramiko)
- pip install unittest-xml-reporting (Install XML Test Runner)
- pip install -U selenium (Installs Selenium)

4) Get PhoantomJS for your OS from http://phantomjs.org/

- PhantomJS will run selenium test in headless mode. Follow the instruction on PhantomJS.org.
- Make sure the executable is in PATH. (TIP: Drop it in Python27 folder :-))

5) Now get the HTMLTestRunner for nice looking report generation.
- http://tungwaiyip.info/software/HTMLTestRunner.html
- Download and put this file into Lib of your python installation.


##########################################################################################################################################

2) Test Prerequisites
---------------------

- Download and install CS. /cwiki.apache.org has links to Installation Guide and API reference.
- Log into the management server and Add a Zone. (Must be Advance Zone and Hypervisor type must be Xen)


##########################################################################################################################################

3) Running the Test and Generating the report
---------------------------------------------

- Folder smoke contains main.py
- main.py is the file where all the tests are serialized.
- main.py supports HTML and XML reporting. Please refer to end of file to choose either.
- Typical usage is:  python main.py 10.1.1.10 >> result.xml for XML Reporting
- And python main.py 10.1.1.10 >> result.html for HTML Reporting.
- 10.1.1.10 (your management server IP) is an argument required for main.

##########################################################################################################################################
