#!/bin/bash

# ------------------------------------------
# Text-Fabric to NER and back
# - extract missives from Text-Fabric,
# - run NER on the text,
# - write predicted entities to TSV and
# - export the annotations to Text-Fabric
# ------------------------------------------

set -e

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)
ddir="$wdir"/data/tf_export

python textfabric/tf2naf.py -o "$ddir"
python ner/src/utils/pipeline.py -i "$ddir"/tf/ -o "$ddir"/ner
python textfabric/tsv2tf.py "$ddir"/tf "$ddir"/ner export/tf
