#!/bin/bash
#
# man-in2naf.sh
#
# Transfers manually annotated entities from XMI or CONLL file to
# reference NAF.
# Corrects for differences in raw text between input and reference files
#---------------------------------------------------------------------------

indir=$1    # manual annotation files (.xmi or .conll)
refdir=$2   # reference naf files
outdir=$3   # output naf directory

if [ $# -ne 3 ]; then
  echo "Usage: sh man-in2naf.sh INPUT_DIR REF_NAF_DIR OUT_NAF_DIR"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir -p $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -r ${refdir} -o ${outdir} -e man


