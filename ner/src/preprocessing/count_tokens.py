import sys
import os


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


def conll_counts(file):
    tokens = 0
    entities = 0
    sequences = read_conll_sequences(file)
    for sequence in sequences:
        tokens += len(sequence)
        entities += len([line for line in sequence if " B-" in line])
    avg_length = tokens * 1.0 / len(sequences)
    return "{} tokens, {} entities, {} avg_length".format(tokens, entities, avg_length)


def stats(indir):
    for texttype in TEXTTYPES:
        for dataset in DATASETS:
            file = os.path.join(indir, texttype, 'raw', "{}.conll".format(dataset))
            print(file)
            print(conll_counts(file))
            print()


if __name__ == "__main__":
    stats(sys.argv[1])