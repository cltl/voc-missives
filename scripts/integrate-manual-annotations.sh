#!/bin/bash
#
# Integration of manual annotations into reference naf files
# The script:
#   1. selects TEI files matching the input annotations from the TEI corpus directory
#   2. converts the selected TEI files to NAF
#   3. integrates the input annotations into the NAF files
#   4. converts the enriched NAF files to Conll (2002)
# ------------------------------------------------------------------------------------


teidir=$1   # location of the TEI corpus directory
wdir=$2     # work directory. This is expected to contain an 'inXMI' directory for XMI input
            #  and an 'inConll' directory for Conll input annotations.

# -- variables ---------------

inxmi=${wdir}/inXmi
inconll=${wdir}/inConll
reftei=${wdir}/refTei
refnaf=${wdir}/refNaf
outnaf=${wdir}/outNaf
outconll=${wdir}/outconll
wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
scripts=${wdir}/scripts

# -- functions ---------------

select_reftei() {
  echo "# step 1: select reference tei files"

  echo $inconll
  if [ -d $inxmi ]; then
    echo " -- selecting matching TEI files for XMI input in: $inxmi"
    sh $scripts/get-matching-tei-files.sh $inxmi/text $teidir $reftei
    sh $scripts/get-matching-tei-files.sh $inxmi/notes $teidir $reftei
  fi
  if [ -d $inconll ]; then
    echo " -- selecting matching TEI files for Conll input in: $inconll"
    sh $scripts/get-matching-tei-files.sh $inconll/text $teidir $reftei
    sh $scripts/get-matching-tei-files.sh $inconll/notes $teidir $reftei
  fi
}

convert_tei2naf() {
  echo "# step 2: convert tei -> naf"
  sh $scripts/convert-tei-naf.sh $reftei $refnaf
}

integrate_xmi2naf() {
  echo "# step 3a: integrate xmi entities (text+notes)"
  sh $scripts/integrate-xmi-naf.sh $inxmi/text $refnaf $outnaf "manual-text-annotations" text
  cp $outnaf/* $refnaf
  sh $scripts/integrate-xmi-naf.sh $inxmi/notes $refnaf $outnaf "manual-notes-annotations" notes
}

integrate_conll2naf() {
  echo "# step 4a: integrate conll entities (text+notes)"
  sh $scripts/integrate-conll-naf.sh $inconll/text $refnaf $outnaf "manual-text-annotations" text
  cp $outnaf/* $refnaf
  sh $scripts/integrate-conll-naf.sh $inconll/notes $refnaf $outnaf "manual-notes-annotations" notes
}

convert_naf2conll() {
  echo "# step 5: convert enriched naf -> conll"
  sh $scripts/convert-naf-conll.sh $outnaf $outconll
}

# -- function calls ---------------

# select_reftei
[[ -d $reftei ]] && convert_tei2naf
[[ -d $inxmi ]] && integrate_xmi2naf
[[ -d $inconll ]] && integrate_conll2naf
[[ -d $outnaf ]] && convert_naf2conll

