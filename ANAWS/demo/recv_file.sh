#!/bin/bash

portNum=1234

OPTIONS=hp:f:

usage() {
cat << EOF
Send a file using netcat

-h          help
-p          source port number (defaul 1234)
-f          name of the file to receive

EOF
}

# Parse command line
while getopts $OPTIONS opt ; do
	case $opt in
	h ) usage ; exit 0 ;;
	p ) portNum=$OPTARG ;;
	f ) filename=$OPTARG ;;
	esac
done
shift $(($OPTIND-1))

if [ -z $filename ] ; then
	echo "ERROR: you must provide a filename"
	usage
	exit 1
fi

nc -l -p $portNum > $filename

exit 0