#!/bin/bash
#
# convert tei -> naf
# Tei files should have an '.xml' extension to be read as such;
# use '-I xml' for files with a different extension
# -----------------------------------------------------------------

teidir=$1
nafdir=$2

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-1.0-SNAPSHOT-jar-with-dependencies.jar

java -jar $jar -t -i ${teidir} -o ${nafdir}