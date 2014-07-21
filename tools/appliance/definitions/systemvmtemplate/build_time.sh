#!/bin/bash

set -e
set -x

function build_time() {
  date > /etc/vagrant_box_build_time
}

return 2>/dev/null || build_time
