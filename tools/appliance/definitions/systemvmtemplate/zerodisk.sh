# Clean up stuff copied in by veewee
rm -fv /root/*.iso
rm -fv /root/base.sh /root/cleanup.sh /root/postinstall.sh /root/zerodisk.sh
rm -fv .veewee_version .veewee_params .vbox_version

echo "Cleaning up"

# Zero out the free space to save space in the final image:
for path in / /boot /usr /var /opt /tmp /home
do
  dd if=/dev/zero of=$path/zero bs=1M
  sync
  rm -f $path/zero
  echo "Completed zero-ing out disk on $path"
done
