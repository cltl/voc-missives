import argparse
import json
import os.path
from pathlib import Path
from typing import List

from transformers import AutoTokenizer

JSONL_FILE = 'input.json'
MAPPING_FILE = 'mapping.jsonl'
RUN_CFG = 'run.cfg'


def split_sequences(conll_file, model_name_or_path, max_len):
    """Adapted from HuggingFace script:
    https://github.com/huggingface/transformers/blob/master/examples/legacy/token-classification/scripts/preprocess.py"""
    subword_len_counter = 0
    tokenizer = AutoTokenizer.from_pretrained(model_name_or_path)
    max_len -= tokenizer.num_special_tokens_to_add()

    sequences = []
    with open(conll_file, "rt") as f_p:
        seq = []
        for line in f_p:
            line = line.rstrip()

            if not line:
                if seq:
                    sequences.append(seq)
                    seq = []
                subword_len_counter = 0
                continue

            token = line.split()[0]
            current_subwords_len = len(tokenizer.tokenize(token))

            if (subword_len_counter + current_subwords_len) > max_len:
                sequences.append(seq)
                seq = [line]
                subword_len_counter = current_subwords_len
                continue

            subword_len_counter += current_subwords_len
            seq.append(line)
    if seq:
        sequences.append(seq)
    return sequences


def as_json(seq):
    tokens = []
    labels = []
    for line in seq:
        tokens.append(line.split()[0])
        labels.append(line.split()[1])
    return {'words': tokens, 'ner': labels}


def process_file(conll_in: str, jsonl: List[dict], mapping: List[dict], model, max_seq_length, offset=0):
    lines = [as_json(seq) for seq in split_sequences(conll_in, model_name_or_path=model, max_len=max_seq_length)]
    jsonl.extend(lines)
    mapping.append({'file': os.path.basename(conll_in), 'begin': offset, 'end': offset + len(lines)})
    return offset + len(lines)


def write_jsonl(jsonl: List[dict], file: str):
    with open(file, mode='w') as f:
        for line in jsonl:
            json.dump(line, f)
            f.write("\n")


def write_runcfg(nerdir, model):
    test_file = os.path.join(nerdir, 'input.json')
    cfg = {"train_file": test_file,
           "validation_file": test_file,
           "test_file": test_file,
           "model_name_or_path": model,
           "output_dir": os.path.join(nerdir, 'out'),
           "do_train": False,
           "do_eval": False,
           "do_predict": True,
           "return_entity_level_metrics": True}
    with open(os.path.join(nerdir, RUN_CFG), mode='w') as f:
        json.dump(cfg, f, indent=2)


def main(conll_in: str, outdir: str, model: str, max_seq_length: int):
    mapping = []
    jsonl = []
    start = 0
    if os.path.isdir(conll_in):
        if not os.path.exists(outdir):
            os.makedirs(outdir)
        for path in Path(conll_in).rglob('*.conll'):
            start = process_file(str(path), jsonl, mapping, model, max_seq_length, start)
    else:
        process_file(conll_in, jsonl, mapping, model, max_seq_length)
    write_jsonl(jsonl, os.path.join(outdir, JSONL_FILE))
    write_jsonl(mapping, os.path.join(outdir, MAPPING_FILE))
    write_runcfg(outdir, model)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare conll input for NER processing:\n'
                                                 '* splits input sequences based on Transformer tokenizer '
                                                 'and maximum sequence length;\n'
                                                 '* concatenates sequences into jsonl, where each line stands for a '
                                                 'text unit or part of it;\n'
                                                 '* maps conll files to the corresponding lines into the jsonl\n'
                                                 '* creates a running config for the model')
    parser.add_argument('-i', '--input', type=str, help='input conll dir/file')
    parser.add_argument('-d', '--outdir', type=str, help='output directory for jsonl file, conll-to-jsonl mapping and '
                                                         'running config')
    parser.add_argument('-t', '--model', type=str, help='Transformer model name or path',
                        default="CLTL/gm-ner-xlmrbase")
    parser.add_argument('-m', '--max_seq_length', type=str, help='maximum subtoken sequence length', default=256)
    args = parser.parse_args()
    main(args.input, args.outdir, args.model, args.max_seq_length)
