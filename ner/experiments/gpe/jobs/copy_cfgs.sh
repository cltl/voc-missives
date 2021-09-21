#!/bin/bash

copy_cfgs() {
  seed=$1
  [[ ! -d seed${seed}/cfg ]] && mkdir -p seed${seed}/cfg
  for f in /voc_ner/resources/cfg/*; do
    fname=$(basename $f)
    sed -e "s:1,:${seed},:" -e "s:seed1:seed${seed}:" $f > seed${seed}/cfg/${fname}
  done
}

copy_cfgs 1
copy_cfgs 10
copy_cfgs 100
