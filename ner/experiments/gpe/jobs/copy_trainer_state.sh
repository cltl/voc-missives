#!/bin/bash

copy_results() {
  mpath=$1
  mname=$2
  cp experiments/notes/$mpath/trainer_state.json results/notes_${mname}_trainer_state.json
  cp experiments/all/$mpath/trainer_state.json results/all_${mname}_trainer_state.json
}

copy_results bert-base-multilingual-cased_256 mbert 
copy_results GroNLP/bert-base-dutch-cased_256 bertje 
copy_results xlm-roberta-base_256 xlmr
copy_results pdelobelle/robbert-v2-dutch-base_256 robbert 
