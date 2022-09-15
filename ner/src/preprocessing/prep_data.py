import argparse
import distutils
import os
import random
import glob
import json

from transformers import AutoTokenizer

from ner.src.utils.conll2ner import split_sequences, as_json, write_jsonl

REMAP = True
CORPUS = 'corpus'
TEXTTYPES = ['text', 'notes', 'all']
DATASETS = ['train', 'dev', 'test']
MODELS = ['bert-base-multilingual-cased', 'GroNLP/bert-base-dutch-cased', 'xlm-roberta-base', 'pdelobelle/robbert-v2-dutch-base']


def read_conll_sequences(file):
    sequences = []
    sentence = []
    with open(file) as f:
        for line in f.readlines():
            if not line.strip():
                if sentence:
                    sequences.append(sentence)
                    sentence = []
            else:
                sentence.append(line.strip())
        if sentence:
            sequences.append(sentence)
    return sequences


def prepare_input(input, remap, output, map_gpe_to_loc):
    """Copies input to output, replacing 'Xpart' and 'REL' labels if 'remap' is True"""
    if remap:
        files = [file for file in glob.iglob("{}/**".format(input), recursive=True) if os.path.isfile(file)]
        for file in files:
            with open(file) as f:
                with open(os.path.join(output, os.path.basename(file)), 'w') as out:
                    for line in f.readlines():
                        if line.strip():
                            line_stripped = line.strip()
                            bio_label = line_stripped.split(' ')[1]
                            label = bio_label[2:]
                            if label == 'RELpart' or label == 'REL':
                                out.write(line.replace(label, 'RELderiv'))
                            elif label == 'LOCpart':
                                out.write(line.replace(label, 'LOCderiv'))
                            elif label == 'ORGpart':
                                out.write(line.replace(label, 'ORG'))
                            elif map_gpe_to_loc and label == 'GPE':
                                out.write(line.replace(label, 'LOC'))
                            else:
                                out.write(line)
                        else:
                            out.write(line)
    else:
        distutils.dir_util.copy_tree(input, output)


def concatenate(files, output):
    with open(output, 'w') as f:
        for file in files:
            with open(file) as infile:
                for line in infile:
                    f.write(line)


def shuffle_and_concatenate(files, output):
    sequences = []
    for file in files:
        sequences.extend(read_conll_sequences(file))
    random.seed(1)
    random.shuffle(sequences)
    with open(output, 'w') as f:
        for seq in sequences:
            for line in seq:
                f.write("{}\n".format(line))
            f.write("\n")


def split_train_dev_test(datasplit, outdir, text_type):
    """Splits data into train/dev/test sets"""
    train = []
    dev = []
    test = []
    with open(datasplit) as f:
        for missive in json.load(f):
            if text_type != 'all' and missive['type'] != text_type:
                continue
            if missive['train_dev_test'] == 'train':
                train.append(os.path.join(outdir, CORPUS, "{}.conll".format(missive['tf_id'])))
            elif missive['train_dev_test'] == 'dev':
                dev.append(os.path.join(outdir, CORPUS, "{}.conll".format(missive['tf_id'])))
            elif missive['train_dev_test'] == 'test':
                test.append(os.path.join(outdir, CORPUS, "{}.conll".format(missive['tf_id'])))
    splitdir = os.path.join(outdir, text_type, 'raw')
    os.makedirs(splitdir, exist_ok=True)
    shuffle_and_concatenate(train, os.path.join(splitdir, 'train.conll'))
    concatenate(dev, os.path.join(splitdir, 'dev.conll'))
    concatenate(test, os.path.join(splitdir, 'test.conll'))


def write2json(sequences, outfile):
    dicts = [as_json(seq) for seq in sequences]
    write_jsonl(dicts, outfile)


def prepare_transformers_input(maxlength, outdir):
    for text_type in TEXTTYPES:
        for dataset in DATASETS:
            datafile = os.path.join(outdir, text_type, 'raw', "{}.conll".format(dataset))
            for model in MODELS:
                sequences = split_sequences(datafile, model, maxlength)
                modeldir = os.path.join(outdir, text_type, "{}_{}".format(model, maxlength))
                os.makedirs(modeldir, exist_ok=True)
                write2json(sequences, os.path.join(modeldir, "{}.json".format(dataset)))


def process(input, outdir, datasplit, maxlength, map_gpe_to_loc):
    os.makedirs(outdir, exist_ok=True)
    corpusdir = os.path.join(outdir, CORPUS)
    os.makedirs(corpusdir, exist_ok=True)
    prepare_input(input, REMAP, corpusdir, map_gpe_to_loc)
    if args.datasplit:
        for text_type in TEXTTYPES:
            split_train_dev_test(datasplit, outdir, text_type)
    prepare_transformers_input(maxlength, outdir)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Prepare data for NER: remap rare labels; split train/dev/test; '
                                                 'split long sequences for LM models')
    parser.add_argument('-i', '--input', type=str, help='input corpus')
    parser.add_argument('-o', '--outdir', type=str, help='output directory')
    parser.add_argument('-d', '--datasplit', type=str, help='json file for train/dev/test split')
    parser.add_argument('-m', '--maxlength', type=int, help='max length for model tokenizer', default=256)
    parser.add_argument('-g', '--map_gpe_to_loc', action='store_true')
    args = parser.parse_args()
    process(args.input, args.outdir, args.datasplit, args.maxlength, args.map_gpe_to_loc)

