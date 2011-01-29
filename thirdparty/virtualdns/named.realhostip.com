; Zone file for realhostip.com
;
; The full zone file
;
@       IN      SOA     realhostip.com. admin.realhostip.com. (
                        201006211       ; serial, todays date + todays serial #
                        8H              ; refresh, seconds
                        2H              ; retry, seconds
                        4W              ; expire, seconds
                        1D )            ; minimum, seconds
                TXT     "realhostip.com"
                NS      ns.realhostip.com.
                NS      ns2.realhostip.com.

localhost       A       127.0.0.1

ns         A       184.72.55.159
ns2        A       184.72.250.40
test       A       1.2.3.4
