#!/bin/bash
#
# runs tei2naf conversion over a directory of tei files with subdirectories.
# this is useful for the missives, which are subdivided by volume
# the assumed structure is input: $1/volume; output: $2/volume
#---------------------------------------------------------------------------

indir=$1
outdir=$2


wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-*-SNAPSHOT-jar-with-dependencies.jar

for subdir in ${indir}/*; do
  volume=$(basename $subdir)
  echo "converting from ${indir}/${volume} to ${outdir}/${volume}"
  java -jar $jar -i ${indir}/${volume} -o ${outdir}/${volume}
done
