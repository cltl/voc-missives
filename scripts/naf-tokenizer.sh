#!/bin/bash
#
# naf-tokenizer.sh
#
# tokenizes input Naf files with tunits. The tunits may be sequential
# or tree-like
# sh naf-tokenizer.sh INPUT_NAF_DIR OUT_NAF_DIR
#---------------------------------------------------------------------------

indir=$1
outdir=$2

if [ $# -ne 2 ]; then
  echo "Usage: sh naf-selector.sh INPUT_NAF_DIR REF_NAF_DIR"
  exit 1
fi

[[ ! -d ${outdir} ]] && mkdir -p ${outdir}

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -o ${outdir} -O naf -t

