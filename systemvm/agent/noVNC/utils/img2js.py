#!/usr/bin/env python

#
# Convert image to Javascript compatible base64 Data URI
# Copyright (C) 2018 The noVNC Authors
# Licensed under MPL 2.0 (see docs/LICENSE.MPL-2.0)
#

import sys, base64

try:
    from PIL import Image
except:
    print "python PIL module required (python-imaging package)"
    sys.exit(1)


if len(sys.argv) < 3:
    print "Usage: %s IMAGE JS_VARIABLE" % sys.argv[0]
    sys.exit(1)

fname = sys.argv[1]
var   = sys.argv[2]

ext = fname.lower().split('.')[-1]
if   ext == "png":            mime = "image/png"
elif ext in ["jpg", "jpeg"]:  mime = "image/jpeg"
elif ext == "gif":            mime = "image/gif"
else:
    print "Only PNG, JPEG and GIF images are supported"
    sys.exit(1)
uri = "data:%s;base64," % mime

im = Image.open(fname)
w, h = im.size

raw = open(fname).read()

print '%s = {"width": %s, "height": %s, "data": "%s%s"};' % (
        var, w, h, uri, base64.b64encode(raw))
