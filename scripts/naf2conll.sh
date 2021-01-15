#!/bin/bash
#
# naf2conll.sh
#
# converts reference NAF to Conll
#---------------------------------------------------------------------------

indir=$1
outdir=$2

if [ $# -ne 2 ]; then
  echo "Usage: sh naf2conll.sh INPUT_NAF_DIR OUT_CONLL_DIR"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir -p $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/gm-processor-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -o ${outdir} -O conll

