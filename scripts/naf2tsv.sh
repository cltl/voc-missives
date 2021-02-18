#!/bin/bash
#
# naf2tsv.sh
#
# converts reference NAF to Tsv for TextFabric
#---------------------------------------------------------------------------

indir=$1
outdir=$2
format=$3   # 'tf' or 'context'

if [ $# -ne 3 ]; then
  echo "Usage: sh naf2tsv.sh INPUT_NAF_DIR OUT_TSV_DIR FORMAT"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir -p $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/gm-processor-*-jar-with-dependencies.jar

if [ "$format" == "tf" ]; then
  java -jar $jar -i ${indir} -o ${outdir} -O tsv -t
else
  java -jar $jar -i ${indir} -o ${outdir} -O tsv
fi

