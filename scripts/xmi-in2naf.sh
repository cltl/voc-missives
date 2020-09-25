#!/bin/bash
#
# xmi-in2naf.sh
#
# transfer entities from XMI file to reference NAF
# sh xmi-in2naf.sh INPUT_XMI_DIR REF_NAF_DIR OUT_NAF_DIR
#---------------------------------------------------------------------------

indir=$1
refdir=$2
outdir=$3

if [ $# -ne 3 ]; then
  echo "Usage: sh xmi-in2naf.sh INPUT_XMI_DIR REF_NAF_DIR OUT_NAF_DIR"
fi

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-SNAPSHOT-jar-with-dependencies.jar

java -jar $jar -i ${indir} -r ${refdir} -o ${outdir}

