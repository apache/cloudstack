@echo on
bcc32.exe -v- -O2 -3 -tWM -xd- -q -w-8066 -c -oomnithread.obj nt.cpp
