#!/bin/bash

# ------------------------------------------
# Text-Fabric to NER and back
# - extract missives from Text-Fabric,
# - run NER on the text,
# - write predicted entities to TSV and
# - export the annotations to Text-Fabric
# ------------------------------------------

set -e

ddir=$1   # data dir, for text/tsv/annotations

wdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)

python textfabric/tf2naf.py -o "$ddir"/tf
python ner/src/utils/pipeline.py -i "$ddir"/tf/tf -o "$ddir"/ner
python textfabric/tsv2tf.py "$ddir"/tf/tf "$ddir"/ner "$ddir"/tf/export
