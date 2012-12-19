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

Your best bet is to copy an existing XML file and fill in your own content between the tags.

At the bottom of this README, there is a fill-in-the-blanks XML template that you can go from. It shows the commonly used tags and explains a bit about how to use them.


----------------------------------

SECTIONS, CHAPTERS, AND BOOK FILES

----------------------------------

The files for every topic and audience are in a single directory. The content is not divided into separate subdirectories for each book, or separate repositories for each book. Therefore, the content can be flexibly and easily re-used. In most cases, a file contains a single section that can be assembled with other sections to build any desired set of information. These files contain <section> ... </section> tags.


Some of the XML files contain only a series of include tags to pull in content from other files. Such an "include file" is either a major section, a chapter in a book, or the master book file. A chapter contains <chapter> ... </chapter> tags.


The master book file contains <book> ... </book> tags. This file is referred to in the Publican configuration file, and is used as the controlling file when building the book.


Document names are derived from the docname setting in the appropriate .cfg file. 
This should not have CloudStack in the name (which is redundant because of 
the CloudStack brand that the documentation is built with. The docname variable
sets the name in the doc site table of contents. This name also needs to exist
as .xml and .ent in the en-US directory. Examples of appropriate docnames: 
Admin_Guide
API_Developers_Guide
Installation_Guide




A Publican book file must also have certain other tags that are expected by
Publican when it builds the project. Copy an existing master book file to
get these tags.


----------------------------------

CONFIG FILES

----------------------------------

For each book file, there must be a corresponding publican.cfg (or
<other_name>.cfg) file in order to build the book with Publican. The
docname: attribute in the config file matches the name of the master book file;
for example, docname: cloudstack corresponds to the master book file 
cloudstack.xml.


The .cfg files reside in the main directory, docs. To build a different book,
just use the Publican command line flag --config=<filename>.cfg. (We also
need per-book entities, Book_Info, Author_Info, and other Publican files.
The technique for pulling these in is TBD.)


----------------------------------

TO BUILD A BOOK

----------------------------------

We will set up an automatic Publican job that generates new output whenever we
check in changes to this repository. You can also build a book locally as 
follows.


First, install Publican, and get a local copy of the book source files.


Put the desired publican.cfg in the docs directory. Go to the command line, cd
to that directory, and run the publican build command. Specify what output 
format(s) and what language(s) you want to build. Always start with a test 
run. For example:


publican build --formats test --langs en-US


...followed by this command if the test is successful:


publican build --formats html,pdf --langs en-US


Output will be found in the tmp subdirectory of the docs directory.



----------------------------------

LOCALIZATION

----------------------------------

Localized versions of the documentation files can be stored in appropriately 
named subdirectories parallel to en-US. The language code names to use for 
these directories are listed in Publican documentation, 
http://jfearn.fedorapeople.org/en-US/Publican/2.7/html/Users_Guide/appe-Users_Guide-Language_codes.html.
For example, Japanese XML files would be stored in the docs/ja-JP directory.

Localization currently happens using Transifex and you can find the strings 
to be translated at this location: 
https://www.transifex.com/projects/p/ACS_DOCS/

In preparation for l10n, authors and docs folks must take not of a number of
things. 
All .xml files must contain a translatable string. <xi:include> tags are not enough. 
All new .xml files must have a corresponding entry in docs/.tx/config 
Filenames should be less than 50 characters long. 

To generate new POT files and upload source do the following: 
publican update_pot --config=./publican-all.cfg
tx push -s 

To receive translated files from publican, run the following command:
tx pull 


----------------------------------

CONTRIBUTING

----------------------------------

Contributors can create new section, chapter, book, publican.cfg, or localized 
.xml files at any time. Submit them following the same patch approval procedure
that is used for contributing to CloudStack code. More information for 
contributors is available at 
https://cwiki.apache.org/confluence/display/CLOUDSTACK/Documentation+Team.

----------------------------------

TAGS FOR A SECTION
----------------------------------


<!DOCTYPE chapter PUBLIC "-//OASIS//DTD DocBook XML V4.5//EN" "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd" [
<!ENTITY % BOOK_ENTITIES SYSTEM "cloudstack.ent">
%BOOK_ENTITIES;
]>

<!-- Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0.
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<!-- Please keep the doctype and license declarations above intact in each doc file.  -->
<!-- Make your modifications below this line.  -->

<section id="INSERT A UNIQUE SECTION ID HERE, PROBABLY MATCHING THE TITLE BELOW. KEEP THE QUOTE MARKS.">
    <title>Text of the section title</title>
    <para>Here's the text of a paragraph in this section.</para>
    <para>Always use &PRODUCT; rather than typing CloudStack.</para>
    <para>Indent with 4 spaces, not with tab characters.</para>
    <para>To hyperlink to a URL outside this document: <ulink url="http://external URL here">Display text of the link here</ulink></para>
    <para>To hyperlink to another section in this document: <xref linkend="SECTION ID OF THE OTHER SECTION GOES HERE." />
        The publication tools will automatically insert the display text of the link for you.</para>
    <note><para>Use this for all tips and asides. Don't use other tags such as tip.
        Our publication tool (publican) prefers the note tag. The tool will 
        automatically insert the text NOTE: for you, so please don't type it.</para></note>
    <warning><para>Use this for anything that is vital to avoid runtime errors. Don't use
        other tags such as caution. Our publication tool (publican) prefers the warning tag. The tool will automatically insert the text WARNING: for you, so please don't type it.</para></warning>
    <para>Here's how to do a bulleted list:</para>
    <itemizedlist>
        <listitem><para>Bulleted list item text.</para></listitem>
    </itemizedlist>
    <para>Here's how to do a numbered list. These are used for step by step instructions 
        or to describe a sequence of events in time. For everything else, use a bulleted list.</para>
    <orderedlist>
        <listitem><para>Text of the step</para></listitem>
        <listitem><para>You might also want a sub-list within one of the list items. Like this:</para>
            <orderedlist numeration="loweralpha">
                <listitem><para>Inner list item text.</para></listitem>
            </orderedlist>
        </listitem>
    </orderedlist>
    <para>Here's how to insert an image. Put the graphic file in images/, a subdirectory of the directory where this XML file is.
        Refer to it using this tag. The tag is admittedly complex, but it's the one we need to use with publican:</para>
    <mediaobject>
        <imageobject>
            <imagedata fileref="./images/YOUR_FILENAME_HERE.png" />
        </imageobject>
        <textobject><phrase>YOUR_FILENAME_HERE.png: Alt text describing this image, such as 
            “structure of a zone.” Required for accessibility.</phrase></textobject>
    </mediaobject>
    <para>A section can contain sub-sections. Please make each sub-section a separate file to enable reuse.
        Then include the sub-section like this:</para>
    <xi:include href="SUBSECTION_FILE_NAME.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />
</section>



----------------------------------

TAGS FOR A CHAPTER
----------------------------------


<!DOCTYPE chapter PUBLIC "-//OASIS//DTD DocBook XML V4.5//EN" "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd" [
<!ENTITY % BOOK_ENTITIES SYSTEM "cloudstack.ent">
%BOOK_ENTITIES;
]>

<!-- Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0.
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<!-- Please keep the doctype and license declarations above intact in each doc file.  -->
<!-- Make your modifications below this line.  -->

<chapter id="INSERT A UNIQUE SECTION ID HERE, PROBABLY MATCHING THE TITLE BELOW. KEEP THE QUOTE MARKS.">
    <title>Text of the chapter title</title>
    <xi:include href="SECTION_ONE_FILENAME.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />
    <xi:include href="SECTION_TWO_FILENAME.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />
</chapter>



----------------------------------

TAGS FOR A BOOK
----------------------------------


<!DOCTYPE chapter PUBLIC "-//OASIS//DTD DocBook XML V4.5//EN" "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd" [
<!ENTITY % BOOK_ENTITIES SYSTEM "cloudstack.ent">
%BOOK_ENTITIES;
]>

<!-- Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0.
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<!-- Please keep the doctype and license declarations above intact in each doc file.  -->
<!-- Make your modifications below this line.  -->

<book>
    <xi:include href="Book_Info.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />
    <xi:include href="CHAPTER_ONE_FILENAME.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />
    <xi:include href="CHAPTER_TWO_FILENAME.xml" xmlns:xi="http://www.w3.org/2001/XInclude" />
</book>

----------------------------------

BASIC RULES FOR INCLUDE STATEMENTS
----------------------------------

A book file must include chapter files.
A chapter file must include section files.
A section file can include other section files, but it doesn't have to.
