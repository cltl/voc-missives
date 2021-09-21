#!/usr/bin/bash

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
[[ ! -d ${workdir}/logs ]] && mkdir ${workdir}/logs

clean-checkpoints() {
  texttype=$1
  model=$2
  if [ "$model" -eq "bertje" ]; then
    rm -rf ${workdir}/experiments/${texttype}/GroNLP/bert-base-dutch-cased_256/checkpoint*
  elif [ "$model" -eq "mbert" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/bert-base-multilingual-cased_256/checkpoint*
  elif [ "$model" -eq "xlmr" ]; then
    rm -rf ${workdir}/experiments/${texttype}/xlm-roberta-base_256/checkpoint*
  elif [ "$model" -eq "robbert" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/pdelobelle/robbert-v2-base_256/checkpoint*
  fi
}

train() {
  texttype=$1
  model=$2
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${workdir}/resources/cfg/train_${texttype}_${model}_256.json \
    &> ${workdir}/logs/train_${texttype}_${model}_256.log
  clean-checkpoints $texttype $model
}

train notes mbert
train notes bertje
train notes xlmr
train notes robbert
train all mbert
train all bertje
train all xlmr
train all robbert

train text mbert
train text bertje
train text xlmr
train text robbert


# run predictions on out-of-domain test data
bash ${workdir}/scripts/run-predict-configs.sh
