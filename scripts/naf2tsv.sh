#!/bin/bash
#
# naf2tsv.sh
#
# converts reference NAF to TSV for TextFabric
#---------------------------------------------------------------------------

indir=$1
outdir=$2
format=$3   # 'tf' (for TextFabric) or 'context' (additionally prints string window for analysis)

if [ $# -ne 3 ]; then
  echo "Usage: sh naf2tsv.sh INPUT_NAF_DIR OUT_TSV_DIR FORMAT"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir -p $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

if [ "$format" == "tf" ]; then
  java -jar $jar -i ${indir} -o ${outdir} -O tsv -f
else
  java -jar $jar -i ${indir} -o ${outdir} -O tsv
fi

