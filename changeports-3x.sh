#!/bin/sh

if [ ! -n "$1" ]
then
    echo "What port number will you be running on?"
    echo "(please use 9001 or above Ex: 11001 or 10001): "
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

