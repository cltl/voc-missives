#!/usr/bin/bash

expdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
experiment=$(basename $expdir)

#[[ ! -d /archive/$experiment ]] && mkdir /archive/$experiment

bash jobs/train_and_predict_gpe.sh seed1
#mv seed1 /archive/$experiment
bash jobs/train_and_predict_gpe.sh seed10
#mv seed10 /archive/$experiment
bash jobs/train_and_predict_gpe.sh seed100
#mv seed100 /archive/$experiment
