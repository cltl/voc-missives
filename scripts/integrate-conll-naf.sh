#!/bin/bash
#
# integrate conll entity annotations -> naf
# Conll files should have an '.conll' extension to be read as such;
# use '-I conll' for files with a different extension
# Likewise, Naf files must have an '.naf' extension;
# use '-R naf' otherwise
# -----------------------------------------------------------------

conlldir=$1
refnafdir=$2
outnafdir=$3
source=$4       # info for NAF header on source of annotations, e.g. "manual notes/text"
type=$5         # the text type of annotations (text or notes)
conllsep=" "    # conll separator (between token and entity; Conll2002 is assumed here)

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-1.0-SNAPSHOT-jar-with-dependencies.jar

java -jar $jar -i ${conlldir} -r ${refnafdir} -o ${outnafdir} -e ${source} -s ${type} -c ${conllsep}