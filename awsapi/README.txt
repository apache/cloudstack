======================================================
Apache Axis2 1.6.2 build (17-04-2012)

http://axis.apache.org/axis2/java/core/
------------------------------------------------------

___________________
Building
===================

We use Maven 2 (http://maven.apache.org) to build, and you'll find a
pom.xml in each module, as well as at the top level.  Use "mvn install"
(or "mvn clean install" to clean up first) to build.

IMPORTANT: the *first* time you build a given version of Axis2, you will not
be able to do a regular "mvn install" from the top level - this is because
we have a couple of custom Maven plugins that (due to some dependency-
resolution issues in Maven) must be built and installed in your local
repository before a build will succeed.  This means you need to do one
of the following:

  1) Use ant (http://ant.apache.org) to build the first time.  There is
     a build.xml at the top level which automatically builds the plugins
     first and then runs a regular "mvn install".
     
  2) Manually "mvn install" both of the plugins in the following places:
     
     modules/tool/axis2-mar-maven-plugin
     modules/tool/axis2-aar-maven-plugin

___________________
Documentation
===================
 
Documentation can be found in the 'docs' distribution of this release 
and in the main site.

___________________
Deploying
===================

To deploy a new Web service in Axis2 the following three steps must 
be performed:
  1) Create the Web service implementation class, supporting classes 
     and the services.xml file, 
  2) Archive the class files into a jar with the services.xml file in 
     the META-INF directory
  3) Drop the jar file to the $AXIS2_HOME/WEB-INF/services directory
     where $AXIS2_HOME represents the install directory of your Axis2 
     runtime. (In the case of a servelet container this would be the
     "axis2" directory inside "webapps".)

To verify the deployment please go to http://<yourip>:<port>/axis2/ and
follow the "Services" Link.

For more information please refer to the User's Guide.

___________________
Support
===================
 
Any problem with this release can be reported to Axis mailing list
or in the JIRA issue tracker. If you are sending an email to the mailing
list make sure to add the [Axis2] prefix to the subject.

Mailing list subscription:
    java-dev-subscribe@axis.apache.org

Jira:
    http://issues.apache.org/jira/browse/AXIS2


Thank you for using Axis2!

The Axis2 Team. 
