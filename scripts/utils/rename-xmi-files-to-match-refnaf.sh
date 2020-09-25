#!/bin/bash
#
# renames XMI files to match reference NAF files
# ------------------------------------------------------

indir=$1    # location of input XMI files
refdir=$2   # location of reference files
outdir=$3   # location for renamed XMI files

[[ ! -d $outdir ]] && mkdir $outdir

for f in $indir/*; do
  filename=$(basename $f)
  pfx=${filename%.xmi}
  echo $pfx
  fileId=${pfx%_notes}
  for naf in $refdir/*${pfx}*; do
    nafname=$(basename $naf)
    nafpfx=${nafname%.naf}
    echo $nafpfx
    cp $f ${outdir}/${nafpfx}.xmi
  done
done

