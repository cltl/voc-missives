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

ddir=$1         # data directory, will contain files resulting from Text-Fabric extraction and NER processing
sdir=$(cd $(dirname "$BASH_SOURCE"[0]) && cd .. && pwd)
wdir=$(cd $(dirname "$BASH_SOURCE"[0]) && cd ../.. && pwd)

cd "$wdir"
pwd
# -- functions --
# Extract letters from text fabric. Naf files with raw text and text units are placed in "$ddir"/basenaf
#python textfabric/tf2naf.py -o "$ddir"

# Tokenize text from basenaf to toknaf folder
#./scripts/naf-tokenizer.sh "$ddir"/basenaf/ "$ddir"/toknaf &>/dev/null

# Convert naf to conll, from toknaf to bareconll
#./scripts/naf2conll-textunits.sh "$ddir"/toknaf/ "$ddir"/bareconll

# Split sequences and convert to json for NER processing
# Defaults to the XLM-Roberta tokenizer used by CLTL/gm-ner-xlmr, and to a max seq length of 256 subtokens
# python ner/src/utils/conll2ner.py -i "$ddir"/bareconll -d "$ddir"/ner

# run Transformer model
python dependencies/transformers/examples/pytorch/token-classification/run_ner.py \
  --model_name_or_path CLTL/gm-ner-xlmrbase \
  --train_file "$ddir"/ner/input.json \
  --validation_file "$ddir"/ner/input.json \
  --test_file "$ddir"/ner/input.json \
  --output_dir "$ddir"/ner/out \
  --do_predict \
  --return_entity_level_metrics &> "$ddir"/ner/run.log

