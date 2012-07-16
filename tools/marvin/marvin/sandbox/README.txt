Welcome to the marvin sandbox
----------------------------------

In here you should find a few common deployment models of CloudStack that you
can configure with properties files to suit your own deployment. One deployment
model for each of - advanced zone, basic zone and a simulator demo are given.  

$ ls -
basic/
advanced/
simulator/

Each property file is divided into logical sections and should be familiar to
those who have deployed CloudStack before. Once you have your properties file
you will have to create a JSON configuration of your deployment using the
python script provided in the respective folder.

The demo files are from the tutorial for testing with python that can be found
on the wiki.cloudstack.org

A common deployment model of a simulator.cfg that can be used for debugging is
included. This will configure an advanced zone with simulators that can be used
for debugging purposes when you do not have hardware to debug with.

To do this:
$ cd cloudstack-oss/
$ ant run-simulator #This will start up the mgmt server with the simulator seeded

## In another shell
$ ant run-simulator
