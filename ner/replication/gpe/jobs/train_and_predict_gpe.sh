#!/usr/bin/bash

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
[[ ! -d ${workdir}/logs ]] && mkdir ${workdir}/logs

resourcedir=/voc_ner/resources

clean-checkpoints() {
  texttype=$1
  model=$2
  if [ "$model" == "bertje" ]; then
    rm -rf ${workdir}/experiments/${texttype}/GroNLP/bert-base-dutch-cased_256/checkpoint*
  elif [ "$model" == "mbert" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/bert-base-multilingual-cased_256/checkpoint*
  elif [ "$model" == "roberta" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/xlm-roberta-base_256/checkpoint*
  elif [ "$model" == "robbert" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/pdelobelle/robbert-v2-dutch-base_256/checkpoint*
  fi
}

predict() {
  model=$1
  trained=$2
  predict=$3
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${resourcedir}/cfg/predict_${predict}_${trained}_${model}.json \
    &> ${workdir}/logs/predict_${predict}_${trained}_${model}.log
}

train() {
  texttype=$1
  model=$2
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${resourcedir}/cfg/train_${texttype}_${model}_256.json \
    &> ${workdir}/logs/train_${texttype}_${model}_256.log
  clean-checkpoints $texttype $model
}

train_and_predict() {
  model=$1
  train notes $model
  train all $model
}

train_and_predict mbert
train_and_predict bertje
train_and_predict roberta
train_and_predict robbert

