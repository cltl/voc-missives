#!/bin/bash
#
# naf-selector.sh
#
# converts inputNAF to referenceNAF based on document type
# sh naf-selector.sh INPUT_NAF_DIR REF_NAF_DIR DOC_TYPE
#---------------------------------------------------------------------------

indir=$1
outdir=$2
doc_type=$3     # text, notes or all

if [ $# -ne 3 ]; then
  echo "Usage: sh naf-selector.sh INPUT_NAF_DIR REF_NAF_DIR DOC_TYPE"
  exit 1
fi

[[ ! -d ${outdir} ]] && mkdir -p ${outdir}

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -o ${outdir} -O naf -d ${doc_type}

