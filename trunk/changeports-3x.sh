#!/bin/sh

# This script adjusts bindings-jboss-beans.xml for a base port of 8001
# to match the out-of-box TRIRIGA configuration in server.xml, and applies
# an optional offset based on the specified desired port.  It is recommended
# that 1000-multiples be used (9001, 10001, etc.) to avoid port collisions.
#
# After running this, the server will start on the specified port base by
# default (e.g. 9001); you can add "-Djboss.service.binding.set=ports-01" to
# the command line to dynamically offset everything by 100 (e.g. 9101), or
# "-Djboss.service.binding.set=ports-02" to offset by 200 (9201), or
# "-Djboss.service.binding.set=ports-03" to offset by 300 (9301).

if [ ! -n "$1" ]
then
    echo "What port number will you be running on?"
    echo "(please use 8001 or above Ex: 9001, 10001, 11001, etc.): "
    read PORT
else
    PORT=$1
fi

let "OFFSET=$PORT - 8001"

for BINDINGS in `find . -name 'bindings-jboss-beans.xml'`
do echo Updating "$BINDINGS"...
sed 's/8080/8001/g
s/5445/5458/g
s/5446/5459/g
s/+ 363/+ 442/g
s/- 71/+ 8/g
s/<parameter>0<\/parameter>/<parameter>'$OFFSET'<\/parameter>/g' "$BINDINGS" > "$BINDINGS.tmp"
mv "$BINDINGS.tmp" "$BINDINGS"
echo Done.

done

