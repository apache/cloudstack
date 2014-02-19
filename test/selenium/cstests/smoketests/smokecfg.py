#!/usr/bin/python
# coding: latin-1

smokecfg = {
            'browser':         'Firefox',
            'window position': '10, 10',        # upper left coordinates
            'window size':     '1200, 1000',
            'cssite':          'http://10.88.91.68:8080/client/',
            'username':        'admin',
            'password':        'password',
            'badusername':     'badname',
            'badpassword':     'badpassword',
            'sqlinjection_1':  '\' or 1=1 --\'',
            'sqlinjection_2':  '\' union select 1, badusername, badpassword, 1--\'',
            'sqlinjection_3':  '\' union select @@version,1,1,1--\'',
            'sqlinjection_4':  '\'; drop table user--\'',
            'sqlinjection_5':  '\'OR\' \'=\'',
            'language':        'English',
           }


