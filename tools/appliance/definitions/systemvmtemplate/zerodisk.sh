#clean up stuff copied in by veewee
rm -f /root/*

# Zero out the free space to save space in the final image:
dd if=/dev/zero of=/EMPTY bs=1M
rm -f /EMPTY

# Shutdown the appliance, now export it to required image format
shutdown -h now
