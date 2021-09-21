#!/bin/bash
#
# naf2conll-textunits.sh
#
# extract NAF entities to Conll, segmenting by text units
#---------------------------------------------------------------------------

indir=$1
outdir=$2

if [ $# -ne 2 ]; then
  echo "Usage: sh naf2conll.sh INPUT_NAF_DIR OUT_CONLL_DIR"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir -p $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

# remove -u to segment by sentences
java -jar $jar -i ${indir} -o ${outdir} -O conll -u

