cd ../../src/
rm -f apis.txt
grep api\( -R . | grep -v import | sed "s/.*api('//g" | sed "s/'.*//g" | grep -v '.vue' | sort | uniq >> apis.txt
grep api -R config | sed "s/.*api: '//g" | sed "s/'.*//g" | grep -v \.js | sort | uniq >> apis.txt
grep store.getters.apis -R . | sed "s/' in.*//g" | sed "s/').*//g" | grep "'" | sed "s/.*'//g" | grep -v ']' >> apis.txt
grep 'permission:\ \[' -R config | sed "s/.*permission: \['//g" | grep -v .js | sed "s/', '/\\n/g" | sed "s/'.*//g" >> apis.txt
cat apis.txt | sort | uniq > apis.uniq
rm -f apis.txt
mv apis.uniq ../tools/api/apis.txt
cd ../tools/api
diff -Naur apis.old apis.txt | grep ^- | grep -v "^--" | sed 's/^-//g' | grep -v -i -e cisco -e nicira -e baremetal -e srx -e f5 -e brocade -e palo -e autoscale -e counter -e condition -e ucs -e netscaler -e bigswitch -e ovs -e globalloadbalancer -e opendaylight -e region -e quota | sort | uniq > apis.remaining

echo "$(cat apis.txt | wc -l) APIs are supported by Primate"
echo "$(cat apis.remaining | wc -l) APIs are remaining"
