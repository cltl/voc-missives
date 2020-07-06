#!/bin/bash
#
# convert naf -> conll
# NAF files should have an '.naf' extension to be read as such;
# use '-I naf' for files with a different extension
# -----------------------------------------------------------------

nafdir=$1
conlldir=$2
conllsep=" "
select=mixed    # choose 'text' or 'notes' to select text type, or 'all' to include forewords

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-1.0-SNAPSHOT-jar-with-dependencies.jar

java -jar $jar -i ${nafdir} -o ${conlldir} -O conll -c ${conllsep} -s ${select}