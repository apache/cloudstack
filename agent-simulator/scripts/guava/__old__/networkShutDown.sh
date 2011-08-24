x=$1
y=$2

for i in `seq $x $y`
do
	stop_vm="GET  http://127.0.0.1/client/?command=stopVirtualMachine&id=$i	HTTP/1.0\n\n"
	echo -e $stop_vm | nc -v -q 60 127.0.0.1 8096
done
