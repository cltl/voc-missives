#!/usr/bin/bash

expdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
workdir=$expdir/$1
[[ ! -d ${workdir}/logs ]] && mkdir ${workdir}/logs


clean-checkpoints() {
  texttype=$1
  model=$2
  if [ "$model" == "bertje" ]; then
    rm -rf ${workdir}/models/${texttype}/GroNLP/bert-base-dutch-cased_256/checkpoint*
  elif [ "$model" == "mbert" ]; then     
    rm -rf ${workdir}/models/${texttype}/bert-base-multilingual-cased_256/checkpoint*
  elif [ "$model" == "xlmr" ]; then     
    rm -rf ${workdir}/models/${texttype}/xlm-roberta-base_256/checkpoint*
  elif [ "$model" == "robbert" ]; then     
    rm -rf ${workdir}/models/${texttype}/pdelobelle/robbert-v2-dutch-base_256/checkpoint*
  fi
}

predict() {
  model=$1
  trained=$2
  predict=$3
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${workdir}/cfg/predict_${predict}_${trained}_${model}.json \
    &> ${workdir}/logs/predict_${predict}_${trained}_${model}.log
}

train() {
  texttype=$1
  model=$2
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${workdir}/cfg/train_${texttype}_${model}_256.json \
    &> ${workdir}/logs/train_${texttype}_${model}_256.log
  clean-checkpoints $texttype $model 
}

train_and_predict() {
  model=$1
  train all $model 
  predict $model all text
  predict $model all notes
  train notes $model
  predict $model notes text
  predict $model notes all
  train text $model
  predict $model text notes
  predict $model text all
}

train_and_predict xlmr 
train_and_predict mbert 
