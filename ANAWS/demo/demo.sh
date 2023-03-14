#! /bin/sh

#start mininet network
sudo mn --custom topology.py --topo fattree --mac --switch ovsk --controller remote,ip=127.0.0.1,port=6653,protocols=OpenFlow13

echo "Enter src"
read src
echo "Enter dest"
read dest
echo "Enter data load (GB)"
read dataload

#open source terminal
xterm $src

#generation of a file with dimension dataload GB
dataload += "G"
truncate -s $dataload test.txt

#configure host to send out traffic for external networks
interface = $src + "-eth0"
route add -net 0.0.0.0/32 dev $interface

#execute python script
res = $(python3 demo.py $dest $dataload)

if [ "$res" == "0" ]; then
    echo "New flow correctly subscribed"
    
    #send file via scp
    scp -v ~/test.txt root@$dest:~/test.txt
else
    echo "No available flow"
fi
