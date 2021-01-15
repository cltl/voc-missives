#!/bin/bash
#
# man-in2naf.sh
#
# Transfers manually annotated entities from XMI or CONLL file to
# reference NAF.
# The input file type is inferred from file extensions (.xmi or .conll)
#---------------------------------------------------------------------------

indir=$1
refdir=$2
outdir=$3

if [ $# -ne 3 ]; then
  echo "Usage: sh man-in2naf.sh INPUT_DIR REF_NAF_DIR OUT_NAF_DIR"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir -p $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/gm-processor-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -r ${refdir} -o ${outdir} -m


