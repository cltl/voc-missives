#!/usr/bin/bash

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
[[ ! -d ${workdir}/logs ]] && mkdir ${workdir}/logs

resourcedir=/voc_ner/resources

clean-checkpoints() {
  texttype=$1
  model=$2
  expdir=$3
  if [ "$model" == "bertje" ]; then
    rm -rf ${expdir}/models/${texttype}/GroNLP/bert-base-dutch-cased_256/checkpoint*
  elif [ "$model" == "mbert" ]; then     
    rm -rf ${expdir}/models/${texttype}/bert-base-multilingual-cased_256/checkpoint*
  elif [ "$model" == "roberta" ]; then     
    rm -rf ${expdir}/models/${texttype}/xlm-roberta-base_256/checkpoint*
  elif [ "$model" == "robbert" ]; then     
    rm -rf ${expdir}/models/${texttype}/pdelobelle/robbert-v2-dutch-base_256/checkpoint*
  fi
}

predict() {
  model=$1
  trained=$2
  predict=$3
  expdir=$4
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${expdir}/cfg/predict_${predict}_${trained}_${model}.json \
    &> ${expdir}/logs/predict_${predict}_${trained}_${model}.log
}

train() {
  texttype=$1
  model=$2
  expdir=$3
  mkdir ${expdir}/logs
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${expdir}/cfg/train_${texttype}_${model}_256.json \
    &> ${expdir}/logs/train_${texttype}_${model}_256.log
  clean-checkpoints ${texttype} ${model} ${expdir}
}

train_and_predict() {
  model=$1
  expdir=$2
  train notes ${model} ${expdir}
  predict ${model} notes text ${expdir}
  predict ${model} notes all ${expdir}
  train text ${model} ${expdir}
  predict ${model} text notes ${expdir}
  predict ${model} text all ${expdir}
  train all ${model} ${expdir}
  predict ${model} all text ${expdir}
  predict ${model} all notes ${expdir}

}

run_experiments() {
  expdir=$1
  train_and_predict mbert ${expdir}
  train_and_predict bertje ${expdir}
  train_and_predict roberta ${expdir}
  train_and_predict robbert ${expdir}
}

expdir=$1   # should contain cfg folder

run_experiments ${expdir}
