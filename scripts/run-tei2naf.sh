#!/bin/bash
#
# runs tei2xmi conversion over a directory of tei files with subdirectories.
# this is useful for the missives, which are subdivided by volume
# the assumed structure is input: $1/tei/volume; output: $1/xmi_1.1/volume
#---------------------------------------------------------------------------

datadir=$1
wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives-1.0-SNAPSHOT-jar-with-dependencies.jar

for subdir in ${datadir}/tei/*; do
  volume=$(basename $subdir)
  java -jar $jar ${datadir}/tei/${volume} ${datadir}/xmi_1.1/${volume}
done
