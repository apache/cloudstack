cd ../../src/
rm -f apis.txt
grep api\( -R . | grep -v import | sed "s/.*api('//g" | sed "s/'.*//g" | grep -v '.vue' | sort | uniq >> apis.txt
grep api -R config | sed "s/.*api: '//g" | sed "s/'.*//g" | grep -v \.js | sort | uniq >> apis.txt
grep store.getters.apis -R . | sed "s/' in.*//g" | sed "s/').*//g" | grep "'" | sed "s/.*'//g" | grep -v ']' >> apis.txt
grep 'permission:\ \[' -R config | sed "s/.*permission: \[ '//g" | grep -v .js | sed "s/', '/\\n/g" | sed "s/'.*//g" >> apis.txt
cat apis.txt | sort | uniq > apis.uniq
mv apis.uniq apis.txt
