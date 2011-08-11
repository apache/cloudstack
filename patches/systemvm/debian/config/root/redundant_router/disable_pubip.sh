#!/bin/bash

ifdown eth2
ifconfig eth2 down
service dnsmasq stop
