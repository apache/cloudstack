#!/bin/bash

# requires netaddr

export PYTHONPATH="../../patches/debian/config/opt/cloud/bin/"
export PYTHONDONTWRITEBYTECODE=False

nosetests .
