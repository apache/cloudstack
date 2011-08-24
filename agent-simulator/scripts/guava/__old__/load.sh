for x in `seq 3 2082`
do
	start_vm="GET  http://127.0.0.1:8096/client/?command=startVirtualMachine&id=$x	HTTP/1.0\n\n"
	echo -e $start_vm | nc -v -q 60 127.0.0.1 8096
done

sleep 60s

for x in `seq 3 1102`
do
	stop_vm="GET  http://127.0.0.1/client/?command=stopVirtualMachine&id=$x	HTTP/1.0\n\n"
	echo -e $stop_vm | nc -v -q 60 127.0.0.1 8096
done

sleep 60s
