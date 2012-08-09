Author: Jessica Tomechak

Updated: August 8, 2012


-------------------------------------------

WHAT'S IN THIS REPOSITORY: WORK IN PROGRESS

-------------------------------------------

This repository contains the source files for CloudStack documentation. The files are currently incomplete as we are in the process of converting documentation from an outdated file format into XML files for this repo.
The complete documentation can be seen at docs.cloudstack.org.



----------------------------------

DOCUMENTATION SUBDIRECTORIES

----------------------------------

United States English language source files are in the en-US subdirectory.
Additional language subdirectories can be added.


Each file in a language subdirectory contains one chunk of information that may be termed a section, module, or topic. The files are written in Docbook XML, using the Docbook version and tag supported by the Publican open-source documentation tool.



----------------------------------

VALID XML TAGS

----------------------------------

Certain tags are disallowed by Publican. Please consult their documentation for more details.
http://jfearn.fedorapeople.org/en-US/Publican/2.7/html/Users_Guide/


----------------------------------

SECTIONS, CHAPTERS, AND BOOK FILES

----------------------------------

The files for every topic and audience are in a single directory. The content is not divided into separate subdirectories for each book, or separate repositories for each book. Therefore, the content can be flexibly and easily re-used. In most cases, a file contains a single section that can be assembled with other sections to build any desired set of information. These files contain <section> ... </section> tags.


Some of the XML files contain only a series of include tags to pull in content from other files. Such an "include file" is either a major section, a chapter in a book, or the master book file. A chapter contains <chapter> ... </chapter> tags.


The master book file contains <book> ... </book> tags. This file is referred to in the Publican configuration file, and is used as the controlling file when building the book.


As a naming convention, start the name of a book file with cloudstack_ ; for example, cloudstack_installation.


A Publican book file must also have certain other tags that are expected by Publican when it builds the project. Copy an existing master book file to get these tags.


----------------------------------

CONFIG FILES

----------------------------------

For each book file, there must be a corresponding publican.cfg (or
<other_name>.cfg) file in order to build the book with Publican. The
docname: attribute in the config file matches the name of the master book file; for example, docname: cloudstack corresponds to the master book file cloudstack.xml.


The .cfg files reside in the main directory, docs. To build a different book, just use the Publican command line flag --config=<filename>.cfg. (We also need per-book entities, Book_Info, Author_Info, and other Publican files. The technique for pulling these in is TBD.)


----------------------------------

TO BUILD A BOOK

----------------------------------

We will set up an automatic Publican job that generates new output whenever we check in changes to this repository. You can also build a book locally as follows.


First, install Publican, and get a local copy of the book source files.


Put the desired publican.cfg in the docs directory. Go to the command line, cd to that directory, and run the publican build command. Specify what output format(s) and what language(s) you want to build. Always start with a test run. For example:


publican build --formats test --langs en-US


...followed by this command if the test is successful:


publican build --formats html,pdf --langs en-US


Output will be found in the /tmp subdirectory.



----------------------------------

LOCALIZATION

----------------------------------

Localized versions of the documentation files can be stored in appropriately named subdirectories parallel to en-US. The language code names to use for these directories are listed in Publican documentation, http://jfearn.fedorapeople.org/en-US/Publican/2.7/html/Users_Guide/appe-Users_Guide-Language_codes.html.
For example, Japanese XML files would be stored in the docs/ja-JP directory.


----------------------------------

CONTRIBUTING

----------------------------------

Contributors can create new section, chapter, book, publican.cfg, or localized .xml files at any time. Submit them following the same patch approval procedure that is used for contributing to CloudStack code. More information for contributors is available at https://cwiki.apache.org/confluence/display/CLOUDSTACK/Documentation+Team.