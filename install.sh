#!/bin/bash

# ----------------------------------------------------------
# installation script: see INSTALL.md for more information
# ----------------------------------------------------------
set -eo pipefail

usage() {
  echo "Usage: $0 [ --java ] [ --textfabric ] [ --transformers ] [ --pipeline ]" 1>&2
  exit 1
}

clean=0
interactive=1
while getopts "ajftp-:" opt; do
  if [ "$opt" == "-" ]; then
    opt="${OPTARG%%=*}"
  fi
  case "$opt" in
    a)
      interactive=0 ;;
    j | java )
      java_flag=1 ;;
    f | textfabric ) 
      tf_flag=1 ;;
    t | transformers ) 
      transformers_flag=1 ;;
    p | pipeline ) 
      pipeline_flag=1 ;;
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

confirm_install() {
  echo "Dependencies will be installed in your current python environment:"
  read -p "Ready (y/n)? " confirm

  if [ "${confirm}" != "y" ]; then
    exit 0
  fi
}

compile_java_code() {
  echo "installing the tokenizer"
  cd ${install_dir}
  [[ ! -d ixa-pipe-tok ]] && git clone https://github.com/ixa-ehu/ixa-pipe-tok.git
  cd ixa-pipe-tok
  git checkout 1ac83fe
  mvn clean install
  echo "compiling and packaging the voc-missives code"
  cd $workdir
  mvn clean package
}

install_textfabric() {
  echo "cloning the TextFabric repositories"
  [[ ! -d ~/github ]] && mkdir -p ~/github/CLARIAH
  git clone https://github.com/CLARIAH/wp6-missieven ~/github/CLARIAH/wp6-missieven
  git clone https://github.com/Dans-labs/clariah-gm ~/github/Dans-labs
  cd ~/github/Dans-labs
  git checkout fc67e0b
  echo "installing python dependencies"
  pip install -r ${workdir}/textfabric/requirements.txt
}

install_transformers() {
  echo "installing Transformers"
  cd ${install_dir}
  [[ ! -d transformers ]] && git clone https://github.com/huggingface/transformers
  cd transformers
  git checkout 626a0a0
  pip install -e .
  cd examples/pytorch/token-classification
  pip install -r requirements.txt
  pip install pytest
}

install_pipeline() {
  pip install transformers[torch]
}

[[ ${interactive} -eq 1 ]] && [[ ${transformers_flag} -eq 1 || ${tf_flag} -eq 1 ]] && confirm_install
[[ ${java_flag} -eq 1 ]] && compile_java_code
[[ ${tf_flag} -eq 1 ]] && install_textfabric
[[ ${transformers_flag} -eq 1 ]] && install_transformers
[[ ${pipeline_flag} -eq 1 ]] && install_pipeline
