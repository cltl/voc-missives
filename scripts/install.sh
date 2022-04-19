#!/bin/bash
set -eo pipefail

usage() {
  echo "Usage: $0 [ --java ] [ --textfabric ] [ --transformers ]" 1>&2
  exit 1
}

clean=0
while getopts "jft-" opt; do
  if [ "$opt" = "-" ]; then
    opt="${OPTARG%%=*}"
  fi
  case "$opt" in
    j | java )
      java_flag=1 ;;
    f | textfabric ) 
      tf_flag=1 ;;
    t | transformers ) 
      transformers_flag=1 ;;
    ??* )
      die "illegal option --$opt" ;;
    *)
      usage ;;
  esac
done
shift $((OPTIND - 1))

workdir=$(cd $(dirname "${BASH_SOURCE[0]}") && cd .. && pwd)
install_dir=${workdir}/dependencies
[[ ! -d ${install_dir} ]] && mkdir ${install_dir}

compile_java_code() {
  # install the tokenizer
  cd ${install_dir}
  git clone https://github.com/ixa-ehu/ixa-pipe-tok.git
  cd ixa-pipe-tok
  git checkout 1ac83fe
  mvn clean install
  # compile and package the voc-missives code
  mvn clean package
}

install_textfabric() {
  # clone TextFabric repositories
  [[ ! -d ~/github/Dans-labs ]] && mkdir -p ~/github/Dans-labs
  [[ ! -d ~/github/annotation ]] && mkdir -p ~/github/annotation
  git clone https://github.com/Dans-labs/clariah-gm ~/github/Dans-labs
  git clone https://github.com/annotation/app-missieven ~/github/annotation
  # install python dependencies
  pip install -r ${workdir}/tf/requirements.txt
}

install_transformers() {
  cd ${install_dir}
  git clone https://github.com/huggingface/transformers
  cd transformers
  git checkout 626a0a0
  pip install
  cd examples/pytorch/token-classification
  pip install -r requirements.txt
}

if [ ${java_flag} -eq 1 ]; then
  compile_java_code
fi
if [ ${tf_flag} -eq 1 ]; then
  install_textfabric
fi
if [ ${transformers_flag} -eq 1 ]; then
  install_transformers
fi
