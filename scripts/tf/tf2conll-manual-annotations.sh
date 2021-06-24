#!/bin/bash
#
# tf2conll-manual-annotations.sh
#
# extracts TextFabric missives based on annotated files
# and outputs them in conll format
#
# runs the following functions:
#   - tf2naf        - missive extraction from TextFabric -> text, pos, basenaf
#   - naf-selector  - tokenization                  -> toknaf
#   - man-in2naf    - conll/xmi into naf            -> mannaf
#   - naf2conll     - (merges overlapping entities) -> manconll
#---------------------------------------------------------------------------

set -e

mandir=$1       # directory containing manual annotations
idmap=$2        # json file with tf ids of missives to extract from TextFabric
ddir=$3         # output directory

sdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)

# -- functions --

tf2naf() {
  echo "exporting missives from TextFabric..."
  cd ${wdir}/tf
  python tf2naf.py $idmap $ddir
}

tokenize() {
  echo "tokenizing base Naf files"
  sh ${sdir}/naf-tokenizer.sh ${ddir}/basenaf ${ddir}/toknaf
}

manin2naf() {
  cd $wdir
  if [ -d ${mandir}/xmi ]; then
    echo "aligning manual Xmi annotations"
    sh ${sdir}/man-in2naf.sh ${mandir}/xmi ${ddir}/toknaf ${ddir}/mannaf
  else
    echo "found no manual Xmi annotations to align"
  fi
  if [ -d ${mandir}/conll ]; then
    echo "aligning manual Conll annotations"
    sh ${sdir}/man-in2naf.sh ${mandir}/conll ${ddir}/toknaf ${ddir}/mannaf
  else
    echo "found no manual Conll annotations to align"
  fi
}

naf2conll() {
  echo "converting Naf entities to Conll"
  sh ${sdir}/naf2conll.sh ${ddir}/mannaf ${ddir}/manconll
}

# -- main --

tf2naf
tokenize
manin2naf
naf2conll

