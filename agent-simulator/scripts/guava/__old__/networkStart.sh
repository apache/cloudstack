x=$1
y=$2

for i in `seq $x $y`
do
	start_vm="GET  http://127.0.0.1:8096/client/?command=startVirtualMachine&id=$i	HTTP/1.0\n\n"
	echo -e $start_vm | nc -v -q 60 127.0.0.1 8096
done
