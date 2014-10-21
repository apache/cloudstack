# Clean up stuff copied in by veewee
rm -fv /root/*.iso
rm -fv /root/base.sh /root/cleanup.sh /root/postinstall.sh /root/zerodisk.sh
rm -fv .veewee_version .veewee_params .vbox_version

echo "Cleaning up"

# Zero out the free space to save space in the final image:
dd if=/dev/zero of=/zero bs=1M
sync
rm -fv /zero
