#!/usr/bin/bash

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
cd $workdir
cd ..
rootdir=$(pwd)
cd $workdir
[[ ! -d ${workdir}/logs ]] && mkdir ${workdir}/logs

resourcedir=${workdir}
transformers=${rootdir}/local/transformers

clean-checkpoints() {
  texttype=$1
  model=$2
  if [ "$model" == "bertje" ]; then
    rm -rf ${workdir}/experiments/${texttype}/GroNLP/bert-base-dutch-cased_64/checkpoint*
  elif [ "$model" == "mbert" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/bert-base-multilingual-cased_64/checkpoint*
  elif [ "$model" == "roberta" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/xlm-roberta-base_64/checkpoint*
  elif [ "$model" == "robbert" ]; then     
    rm -rf ${workdir}/experiments/${texttype}/pdelobelle/robbert-v2-dutch-base_64/checkpoint*
  fi
}

predict() {
  model=$1
  trained=$2
  predict=$3
  python ${transformers}/examples/pytorch/token-classification/run_ner.py \
    ${resourcedir}/cfg/predict_${predict}_${trained}_${model}_test.json \
    &> ${workdir}/logs/predict_${predict}_${trained}_${model}_test.log
}

train() {
  texttype=$1
  model=$2
  python ${transformers}/examples/pytorch/token-classification/run_ner.py \
    ${resourcedir}/cfg/train_${texttype}_${model}_test.json \
    &> ${workdir}/logs/train_${texttype}_${model}_test.log
  clean-checkpoints $texttype $model
}


train notes roberta
predict roberta notes text
