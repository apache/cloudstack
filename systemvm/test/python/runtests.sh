#!/bin/bash

# requires netaddr

export PYTHONPATH="../../patches/debian/config/opt/cloud/bin/"
export PYTHONDONTWRITEBYTECODE=False

pep8 --max-line-length=179 --exclude=monitorServices.py,baremetal-vr.py `find ../../patches -name \*.py`
pep8 --max-line-length=179 *py

nosetests .
