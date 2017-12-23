firewall {
    all-ping enable
    broadcast-ping disable
    config-trap disable
    ipv6-receive-redirects disable
    ipv6-src-route disable
    ip-src-route disable
    log-martians enable
    receive-redirects disable
    send-redirects enable
    source-validation disable
    syn-cookies enable
    twa-hazards-protection disable
}
interfaces {
    ethernet eth0 {
        duplex auto
        smp-affinity auto
        speed auto
        vif 2 {
            address 192.168.2.91/24
        }
    }
    loopback lo {
    }
}
service {
    ssh {
        port 22
    }
}
system {
    config-management {
        commit-revisions 20
    }
    gateway-address 192.168.2.1
    host-name vyos
    login {
        user vyos {
            authentication {
                encrypted-password $6$nJEIsPOST9$BcvgGh7GkjVsne0BDRrD8JI4TYMiBla7mvgbTGh2Bq7w9xdHFVuDBxxtZn6ijkveVk1Mp6qQYCqnsZg1sRulI1
                plaintext-password ""
            }
            level admin
        }
    }
    name-server 192.168.2.1
    ntp {
        server 0.pool.ntp.org {
        }
        server 1.pool.ntp.org {
        }
        server 2.pool.ntp.org {
        }
    }
    syslog {
        global {
            facility all {
                level notice
            }
            facility protocols {
                level debug
            }
        }
    }
    time-zone UTC
}


/* Warning: Do not remove the following line. */
/* === vyatta-config-version: "cluster@1:config-management@1:conntrack-sync@1:conntrack@1:cron@1:dhcp-relay@1:dhcp-server@4:firewall@5:ipsec@4:nat@4:qos@1:quagga@2:system@7:vrrp@1:wanloadbalance@3:webgui@1:webproxy@1:zone-policy@1" === */
/* Release version: 999.201701250336 */
