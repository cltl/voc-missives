#!/bin/bash
#
# manual-annotation-counts.sh
#
# Count entities and tokens in manual annotations
#   - works for naf, xmi and conll input files
#   - reports to system output
#---------------------------------------------------------------------------

indir=$1

if [ $# -ne 1 ]; then
  echo "Usage: sh manual-annotation-counts.sh INPUT_DIR"
  exit 1
fi

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -a manual

