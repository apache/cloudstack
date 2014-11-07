echo "Updating proc/cmdline"
cat <<"EOF" > /etc/default/grub
GRUB_DEFAULT=0
GRUB_TIMEOUT=0
GRUB_DISTRIBUTOR=`lsb_release -i -s 2> /dev/null || echo Debian`
GRUB_CMDLINE_LINUX="debian-installer=en_US%template=domP%name=r-223-VM%vmpassword=password%eth2ip=192.168.30.10%eth2mask=255.255.255.0%gateway=192.168.20.2%eth0ip=10.1.1.10%eth0mask=255.255.255.0%redundant_router=0%guestgw=10.1.1.1%guestbrd=10.1.1.255%guestcidrsize=24%router_pr=100%domain=csa8cloud.internal%dhcprange=10.1.1.101%eth1ip=192.168.22.214%eth1mask=255.255.255.0%type=router%disable_rp_filter=true%dns1=8.8.8.8%dns2=8.8.4.4"
#GRUB_CMDLINE_LINUX_DEFUALT="debian-installer=en_US"
EOF
cat /proc/cmdline
update-grub
