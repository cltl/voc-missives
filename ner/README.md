# NER with pretrained language models

## Prerequisites
See [INSTALL.md](../INSTALL.md#installing-transformers) for installation instructions.

## Data preparation
The corpus is provided under `voc-missives/data/ner/corpus`. 

The script `./scripts/preprocessing/prep_data.py` performs the following operations:
* the data are split in train/dev/test sets, following `./resources/datasplit.json` 
* long sequences are split to a maximum length (`-m` flag, 256 subtokens per default). Note that each pretrained language 
model has its own tokenizer, so that this operation is model specific.
* optionally (`-g` flag), GPE labels are remapped to LOC. Rare labels are automatically remapped (LOCpart -> LOCderiv; ORGpart -> ORG; REL and RELpart -> RELderiv)


## Fine-tuning Test
You can test your installation with the script and config files in the `./test` folder. 
These will let you fine-tune XLM-R-base on 
the notes and test the resulting model on both the notes and text test sets.

First, run the data preparation script
```bash
python ./scripts/preprocessing/prep_data.py -i ../data/ner/corpus -o test/data -d resources/datasplit.json -m 64
```
This will create a `./data` folder under `./test` containing:
* `corpus`: copy of the corpus with remapped labels (rare labels and optionally GPE)
* `all`, `test`, `notes`: datasets for the different types of text, with
  * `raw`: dataset split in train/dev/test
  * datasets prepared for various pretrained models

You can then run `./test/jobs/train_and_predict_test.sh` to fine-tune XLM-R-base on the notes and prediction on notes and text:
```bash
cd test
bash jobs/train_and_predict_test.sh &
```
The config files for training fine-tune for a single epoch, using only part of the datasets (400 training samples and 40 dev/test samples).
Training and prediction are logged under `./test/logs`. 
This should take 5 to 10mn on a laptop, and result in 31.17 F1 on the notes and 32.26 on the text.

## Replicating experiments
The `./experiments` folder contains job scripts and config files (per seed) for replication. 
You will need to adapt paths to your own working directory.

### Standard NER
*(no distinction between LOC and GPE labels)*

Prepare the data:
```jshelllanguage
python ./scripts/preprocessing/prep_data.py -i ../data/ner/corpus -o experiments/loc/data -d resources/datasplit.json -m 256 -g
``` 

See the `./experiments/loc` folder for config files and fine-tuning scripts.

### NER with metonymical use of locations
*(distinction between LOC and GPE labels, where GPE labels identify agent-like use of locations)*

Prepare the data:
```jshelllanguage
python ./scripts/preprocessing/prep_data.py -i ../data/ner/corpus -o experiments/loc/data -d resources/datasplit.json -m 256
``` 

See the `./experiments/gpe` folder for config files and fine-tuning scripts.
