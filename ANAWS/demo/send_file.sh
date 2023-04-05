#!/bin/bash

portNum=1234
timeout=3

OPTIONS=hd:p:f:w:

usage() {
cat << EOF
Send a file using netcat

-h          help
-d          destination IPv4 address
-p          destination port number (defaul 1234)
-f          name of the file to send
-w          connection timeout (default 3s)

EOF
}

# Parse command line
while getopts $OPTIONS opt ; do
	case $opt in
	h ) usage ; exit 0 ;;
	d ) destAddr=$OPTARG ;;
	p ) portNum=$OPTARG ;;
	f ) filename=$OPTARG ;;
    w ) timeout=$OPTARG ;;
	esac
done
shift $(($OPTIND-1))

if [ -z $destAddr ] ; then
	echo "ERROR: you must provide a destination IPv4 address"
	usage
	exit 1
fi

if [ -z $filename ] ; then
	echo "ERROR: you must provide a filename"
	usage
	exit 1
fi

nc -w $timeout $destAddr $portNum < $filename

exit 0