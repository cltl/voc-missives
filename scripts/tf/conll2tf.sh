#!/bin/bash
#
# conll2tf.sh
#
# reads system Conll files into their Naf reference, outputs TSV files
# for TextFabric, and creates TextFabric entity features for the Missieven App
#
# runs the following functions:
#   - conll-in2naf  - adds conll entities to reference naf  -> sysnaf
#   - naf2tsv       - translates Naf entities to TSV        -> tsv
#   - tsv2tf        - adds entity features to local TF app  -> tf files
#---------------------------------------------------------------------------

set -e

ddir=$1         # data directory, should contain the following folders:
                # tf (text and pos), toknaf
conlldir=$2

sdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)

# -- functions --

sysin2naf() {
  sh ${sdir}/sys-in2naf.sh ${conlldir} ${ddir}/toknaf ${ddir}/sysnaf
}

naf2tsv() {
  sh ${sdir}/naf2tsv.sh ${ddir}/sysnaf ${ddir}/tsv tf
}

tsv2tf() {
  cd ${wdir}/textfabric
  python tsv2tf.py ${ddir}/tf ${ddir}/tsv ${wdir}/export/tf
}


# -- main --

sysin2naf
naf2tsv
tsv2tf

