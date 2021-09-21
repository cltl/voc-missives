#!/usr/bin/bash

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. & pwd)
[[ ! -d ${workdir}/logs ]] && mkdir ${workdir}/logs

predict() {
  model=$1
  trained=$2
  predict=$3
  python /data/transformers/examples/pytorch/token-classification/run_ner.py \
    ${workdir}/resources/cfg/predict_${predict}_${trained}_${model}.json \
    &> ${workdir}/logs/predict_${predict}_${trained}_${model}.log
}

for model in mbert bertje xlmr robbert; do
  predict $model text notes
  predict $model text all
done

for model in mbert bertje xlmr robbert; do
  predict $model notes text
  predict $model notes all
done

for model in mbert bertje xlmr robbert; do
  predict $model all text
  predict $model all notes
done
