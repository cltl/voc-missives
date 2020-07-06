#!/bin/bash
#
# file name matcher for the missives
# selects reference tei files based on input file names
# assumes the tei files are subdivided by volume, and carry the extension '.xml'
# ------------------------------------------------------

indir=$1    # location of input files
refdir=$2   # location for reference files subdirectories
outdir=$3   # location for selected reference files

[[ ! -d $outdir ]] && mkdir $outdir

for f in $indir/*; do
  filename=$(basename $f)
  pfx=${filename%.[a-z][a-z]*}
  fileId=${pfx%_notes}
  for subdir in $refdir/**; do
    [[ -f ${subdir}/${fileId}.xml ]] && cp ${subdir}/${fileId}.xml $outdir
  done
done

