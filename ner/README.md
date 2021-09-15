# NER with pretrained language models

## Prerequisites
Install the HuggingFace Transformers library (git checkout 626a0a0 was used for experiments)

```bash
mkdir local
cd local
git clone https://github.com/huggingface/transformers
cd transformers
pip install
cd examples/pytorch/token-classification
pip install -r requirements.txt
```

## Data preparation
The corpus is provided under `voc-missives/data/ner/corpus`. 

The script `./scripts/preprocessing/prep_data.py` performs the following operations:
* the data are split in train/dev/test sets, following `./resources/datasplit.json` 
* long sequences are split to a maximum length (`-m` flag, 256 subtokens per default). Note that each pretrained language 
model has its own tokenizer, so that this operation is model specific.
* optionally (`-g` flag), GPE labels are remapped to LOC. Rare labels are automatically remapped (LOCpart -> LOCderiv; ORGpart -> ORG; REL and RELpart -> RELderiv)


## Fine-tuning Test
The `./test` folder contains config files for different pretrained models, and a test job to fine-tune one of these models on 
the notes and test it on both the notes and text test sets.

First, run the data preparation script
```bash
python ./scripts/preprocessing/prep_data.py -i ../data/ner/corpus -o test/data -d resources/datasplit.json -m 64
```
This will create a `./data` folder under `./test` containing:
* `corpus`: copy of the corpus with remapped labels (rare labels and optionally GPE)
* `all`, `test`, `notes`: datasets for the different types of text, with
  * `raw`: dataset split in train/dev/test
  * datasets prepared for various pretrained models

You can then run `./test/jobs/train_and_predict_test.sh` to fine-tune XLM-RoBERTa on the notes and prediction on notes and text:
```bash
cd test
bash jobs/train_and_predict_test.sh &
```
The config files for training fine-tune for a single epoch, using only part of the datasets (400 training samples and 40 dev/test samples).
Training and prediction are logged under `logs`. 
This should take 5 to 10mn on a laptop, and result in 31.17 F1 on the notes and 32.26 on the text.

## Replication experiments
The `./replication` folder contains job scripts for replication. Config files are located und `./resources/cfg`. 
The fine-tuning scripts can run both on CPU or GPU. The first experiment takes about 2 hours on a GPU with 12GB RAM, and
 the second experiment about 1.5 hour. 
Reference results and logs are provided in each experiment folder (`ref_results` and `ref_logs`).

### cross_text_loc

**Goal** 
crossed fine-tuning and testing on text, notes or both parts of missives, whereby the GPE label is remapped to LOC.

**Prepare the data**
```bash
python ./scripts/preprocessing/prep_data.py -i ../data/ner/corpus -o replication/cross_text_loc/data -d resources/datasplit.json -m 256 -g
```
**NER system training**
NOTE You should adapt the paths to the resources directory and the Transformers library in `./jobs/cross_train_and_predict.sh`.
```
cd ./replication/cross_text_loc
bash jobs/cross_train_and_predict.sh &
```

### gpe 

**Goal** 
Fine-tuning on notes and both parts of missives, keeping the GPE label.

**Prepare the data**
```bash
python ./scripts/preprocessing/prep_data.py -i ../data/ner/corpus -o replication/gpe/data -d resources/datasplit.json -m 256
```
**NER system training**
NOTE You should adapt the paths to the resources directory and the Transformers library in `./jobs/cross_train_and_predict.sh`.
```
cd ./replication/gpe
bash jobs/train_and_predict_gpe.sh &
``` 