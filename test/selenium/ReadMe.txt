##############################################
This files contains following:

1) Installation requirements
2) Test Pre requisites
3) Running the Test and Generating the report
##############################################



##########################################################################################################################################

1) Installtion Requirements


1)Firefox depending on your OS (Good to have Firebug and Selenium IDE for troubleshooting and dev work)


2)Install Python 2.7. Recommend to use Active State Python


3) Now Open CMD/Terminal and type all of following

- pypm install pycrypto (Installs Pycrypto)
- pypm install paramiko (Install paramiko)
- pip install unittest-xml-reporting (Install XML Test Runner)
- pip install -U selenium (Installs Selenium)


5) Now get the HTMLTestRunner for nice looking report generation.
- http://tungwaiyip.info/software/HTMLTestRunner.html
- Download and put this file into Lib of your python installation.


##########################################################################################################################################

2) Test Prerequisites

- Download and install CS
- Log into the management server nad Add a Zone. (Must be Advance Zone and Hypervisor type must be Xen)

##########################################################################################################################################

3) Running the Test and Generating the report

- Folder smoke contains main.py
- main.py is the file where all the tests are serialized.
- main.py supports HTML and XML reporting. Please refer to end of file to choose either.
- Typical usage is:  python main.py for XML Reporting
- And python main.py >> results.html for HTML Reporting.

##########################################################################################################################################
