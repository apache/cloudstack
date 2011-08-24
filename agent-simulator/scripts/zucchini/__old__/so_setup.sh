#setup service offerings with hosttags - TAG1, TAG2, TAG3
#offering sizes are such that only one VM sits on each host
host=$1

./setupServiceOffering.sh -h $host -i T1 -l -m 7168 -c 1024 -n 1 -g TAG1
./setupServiceOffering.sh -h $host -i T2 -l -m 7168 -c 1024 -n 1 -g TAG2
./setupServiceOffering.sh -h $host -i T3 -l -m 7168 -c 1024 -n 1 -g TAG3
