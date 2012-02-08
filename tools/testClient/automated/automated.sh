#!/bin/sh

export CATALINA_HOME=/usr/share/tomcat6

ant clean-all

ant automated-test-run

exit $?
