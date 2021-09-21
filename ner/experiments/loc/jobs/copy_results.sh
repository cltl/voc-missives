#!/bin/bash

workdir=/archive/loc
[[ ! -d results ]] && mkdir results

copy_results() {
  mpath=$1
  mname=$2
  for data in all notes text; do
    for expdir in seed1 seed10 seed100; do
      cp ${workdir}/$expdir/models/$data/$mpath/predict_results.json results/${expdir}_${data}_${mname}_${data}_predict_results.json
      cp ${workdir}/$expdir/models/$data/$mpath/predictions.txt results/${expdir}_${data}_${mname}_${data}_predictions.txt
      cp ${workdir}/$expdir/models/$data/$mpath/trainer_state.json results/${expdir}_${data}_${mname}_trainer_state.json
      for tdata in all notes text; do
	if [ "$tdata" != "$data" ]; then
          cp ${workdir}/$expdir/models/$data/$mpath/$tdata/predict_results.json results/${expdir}_${data}_${mname}_${tdata}_predict_results.json
          cp ${workdir}/$expdir/models/$data/$mpath/$tdata/predictions.txt results/${expdir}_${data}_${mname}_${tdata}_predictions.txt
	fi
      done
    done
    cp data/$data/$mpath/test.json results/${data}_${mname}_test.json
  done
}

copy_model_results() {
  copy_results bert-base-multilingual-cased_256 mbert 
  copy_results GroNLP/bert-base-dutch-cased_256 bertje 
  copy_results xlm-roberta-base_256 xlmr 
  copy_results pdelobelle/robbert-v2-dutch-base_256 robbert 
} 

copy_model_results
