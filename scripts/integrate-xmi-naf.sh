#!/bin/bash
#
# integrate xmi entity annotations -> naf
# Xmi files should have an '.xmi' extension to be read as such;
# use '-I xmi' for files with a different extension
# Likewise, Naf files must have an '.naf' extension;
# use '-R naf' otherwise
# -----------------------------------------------------------------

xmidir=$1
refnafdir=$2
outnafdir=$3
source=$4       # info for NAF header on source of annotations, e.g. "manual notes/text"
type=$5         # the text type of annotations (text or notes)

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-1.0-SNAPSHOT-jar-with-dependencies.jar

java -jar $jar -i ${xmidir} -r ${refnafdir} -o ${outnafdir} -e ${source} -s ${type}