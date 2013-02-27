# Clean up stuff copied in by veewee
rm -f /root/*

echo "Cleaning up"

# Zero out the free space to save space in the final image:
for path in / /boot /usr /var /opt /tmp
do
  dd if=/dev/zero of=$path/zero bs=1M
  sync
  rm -f $path/zero
  echo "Completed zero-ing out disk on $path"
done
