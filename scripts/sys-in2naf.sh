#!/bin/bash
#
# sys-in2naf.sh
#
# Adds entities from system CONLL file to reference NAF
# (Existing entities are rewritten)
#
#---------------------------------------------------------------------------

indir=$1    # input conll files
refdir=$2   # reference naf files
outdir=$3   # output naf directory
version=0   # revision version for entities linguistic processor in NAF header

if [ $# -ne 3 ]; then
  echo "Usage: sh sys-in2naf.sh INPUT_DIR REF_NAF_DIR OUT_NAF_DIR VERSION"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -r ${refdir} -o ${outdir} -e sys -v $version

