#!/usr/bin/bash

expdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
experiment=$(basename $expdir)

[[ ! -d /archive/$experiment ]] && mkdir /archive/$experiment

move_files() {
  seeddir=$1
  for data in all text notes; do
    for model in $seeddir/models/$data/*; do
      mv $model /archive/$experiment/$seeddir/models/$data
    done
  done
}

bash jobs/train_and_predict_nl.sh seed1
move_files seed1
bash jobs/train_and_predict_nl.sh seed10
move_files seed10
bash jobs/train_and_predict_nl.sh seed100
move_files seed100
