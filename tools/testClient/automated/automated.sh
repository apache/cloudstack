#!/bin/sh

export CATALINA_HOME=/usr/share/cloud

ant clean-all

ant automated-test-run

exit $?
