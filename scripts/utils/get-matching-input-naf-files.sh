#!/bin/bash
#
# File name matcher for the missives.
# Used for selecting input NAF files matching the annotated XMI missives
# Assumes the input NAF files directory is subdivided by volume.
# ------------------------------------------------------

indir=$1    # location of input XMI files
refdir=$2   # location of input NAF files
outdir=$3   # location for selected input NAF files

[[ ! -d $outdir ]] && mkdir $outdir


for f in $indir/*; do
  filename=$(basename $f)
  pfx=${filename%.[a-z][a-z]*}
  fileId=${pfx%_notes}          # removes '_notes' extension from input XMI
  for subdir in $refdir/**; do
    if [ -f ${subdir}/*_${fileId}.naf ]; then
      fname=$(basename ${subdir}/*_${fileId}.naf)   # find matching input NAF
      echo $fname
      cp ${subdir}/$fname $outdir
    fi
  done
done

