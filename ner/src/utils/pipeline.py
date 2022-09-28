import argparse
import os.path
import re
from pathlib import Path

from transformers import AutoTokenizer, AutoModelForTokenClassification
from transformers import pipeline

tokenizer = AutoTokenizer.from_pretrained("CLTL/gm-ner-xlmrbase")
model = AutoModelForTokenClassification.from_pretrained("CLTL/gm-ner-xlmrbase")
ner = pipeline("ner", model=model, tokenizer=tokenizer, aggregation_strategy="simple")

file_id_pattern = re.compile(r'missive_(\d+)_(\d+)_(\w+)')
base_file_id_pattern = re.compile(r'missive_(\d+)_(\d+)')

def get_entity_prefix(name):
    m = re.match(file_id_pattern, name)
    if m is None:
        m = re.match(base_file_id_pattern, name)
        return f'e_{m.group(1)}_{m.group(2)}'
    return f'e_{m.group(3)[0]}_{m.group(1)}_{m.group(2)}'


def tsv_line(entity, i, file_pfx):
    start = entity['start']
    end = entity['end']
    label = entity['entity_group']
    entity_id = f'{file_pfx}_{i}'
    return f'{start}\t{end}\t{entity_id}\t{label}\n'


def tsv_header():
    return 'begin\tend\tentityId\tentityKind\n'


def extract_entities(path):
    with open(str(path)) as f:
        text = f.read()
    return ner(text)


def to_tsv(entities, filename, outdir):
    file_pfx = get_entity_prefix(filename)
    tsv_lines = [tsv_line(e, i, file_pfx) for i, e in enumerate(entities)]
    with open(os.path.join(outdir, f'{filename}.tsv'), 'w') as f:
        f.write(tsv_header())
        for line in tsv_lines:
            f.write(line)


def main(input: str, outdir: str):
    os.makedirs(outdir, exist_ok=True)
    paths = [p for p in Path(input).rglob('*') if not p.name.endswith('.pos')]
    for p in paths:
        entities = extract_entities(p)
        to_tsv(entities, p.name, outdir)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run transformer pipeline on text and outputs entities TSV:')
    parser.add_argument('-i', '--input', type=str, help='input text-fabric directory')
    parser.add_argument('-o', '--outdir', type=str, help='output tsv directory')
    args = parser.parse_args()
    main(args.input, args.outdir)
