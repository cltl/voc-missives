#!/bin/bash
#
# runs tei2naf conversion over a directory of tei files with subdirectories.
# this is useful for the missives, which are subdivided by volume
# the assumed structure is input: $1/tei/volume; output: $1/naf/volume
#---------------------------------------------------------------------------

datadir=$1
wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-0.1-SNAPSHOT-jar-with-dependencies.jar

for subdir in ${datadir}/tei/*; do
  volume=$(basename $subdir)
  java -jar $jar ${datadir}/tei/${volume} ${datadir}/naf/${volume}
done
