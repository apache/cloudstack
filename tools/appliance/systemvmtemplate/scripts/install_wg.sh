#!/usr/bin/env bash

set -e
set -x

function install_wg_srv() {
    apt-get install -y wireguard
}

## main ##
install_wg_srv