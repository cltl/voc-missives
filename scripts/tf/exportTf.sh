#!/bin/bash
#
# export2tf.sh
#
# Produces an export of entity features to tf in the export/tf directory of
# this repo.
#
# Consists of a call of the conll2tf.sh script with the right parameters.
#

wdir="$(cd $(dirname "${BASH_SOURCE[0]}") && cd ../.. && pwd)/data"

sh conll2tf.sh "$wdir/tf_transfer_and_revision" "$wdir/ner/corpus"
