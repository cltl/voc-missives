#!/bin/bash
#
# manual-agreement.sh
#
# prints F score IAA and agreement report
#---------------------------------------------------------------------------

# assumes an input directory containing exactly two annotator folders,
# with each folder containing identically named files
indir=$1


if [ $# -ne 1 ]; then
  echo "Usage: sh manual-agreement.sh INPUT_DIR"
  exit 1
fi

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)
jar=${wdir}/target/voc-missives-*-jar-with-dependencies.jar

java -jar $jar -i ${indir} -a agreement

