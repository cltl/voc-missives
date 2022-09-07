#!/bin/bash
#
# tf2ner.sh
#
# Extracts all letters from TextFabric, tokenizes the text in the resulting base NAF files,
# and outputs connl files for NER prediction
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

cd $wdir
# -- functions --
# Extract letters from text fabric. Naf files with raw text and text units are placed in ddir/basenaf
python textfabric/tf2naf.py -o ${ddir}

# Tokenize text from basenaf to toknaf folder
./scripts/naf-tokenizer.sh ${ddir}/basenaf/ ${ddir}/toknaf &>/dev/null
