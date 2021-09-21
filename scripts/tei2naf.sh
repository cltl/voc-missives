#!/bin/bash
#
# runs tei2naf conversion over a directory of tei files with subdirectories.
#---------------------------------------------------------------------------

indir=$1        # input directory, with volume subdirectories
outdir=$2       # output naf directory

if [ $# -ne 2 ]; then
  echo "Usage: sh tei2naf.sh TEI_DIR OUT_NAF_DIR"
  exit 1
fi

[[ ! -d $outdir ]] && mkdir $outdir

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
jar=${wdir}/target/voc-missives*-jar-with-dependencies.jar

for subdir in ${indir}/*; do
  volume=$(basename $subdir)
  echo "converting from ${indir}/${volume} to ${outdir}"
  java -jar $jar -i ${indir}/${volume} -I tei -o ${outdir}
done
