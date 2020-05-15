#!/usr/bin/env python3

from .utilities import bash
import logging

cmd = bash("route -n|awk \'/^0.0.0.0/ {print $2,$8}\'")
if not cmd.isSuccess():
    logging.debug("Failed to get default route")
    raise CloudRuntimeException("Failed to get default route")

result = cmd.getStdout().split(" ")