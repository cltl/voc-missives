# missives data

The data consist of manual annotations, NER training data and intermediary files:
* [manual](#manual-annotations): manual annotations
* [ner](#ner): NER training data
* [tf_export](#text-fabric-annotation-loop): text extraction and NER annotation from and back into Text-Fabric
* [tf_transfer_and_revision](#textfabric-transfer-and-revision): preprocessing data for NER training 

## Manual annotations
Most annotations were performed on raw text and saved as XMI files. 
One missive was first processed with an early NER system, corrected by the annotators, and then saved as Conll. 
 
## TextFabric transfer and revision 
The subfolders in this directory were obtained in this order:

* `tf` missives text extracted from TF with node ids (`.pos` files)
* `basenaf` formatting into NAF (raw text and text units)
* `toknaf` tokenization (adds tokens `text` layer to 'basenaf' files)
* `mannaf` manual entity annotations (from `.data/manual/annotations`) transfered to TextFabric raw text (adds `entities` layer to 'toknaf' files)
* `corrconll` entity correction in conll files derived from mannaf 
* `corrnaf` entity layer in `mannaf` files replaced by corrected entities from `corrconll`
* `sysnaf` NER annotations with fine-tuned model
* `tsv` NER annotations with fine-tuned model (input to Text-Fabric)

## NER
* `corpus` conll files derived from `corrnaf` (`./data/tf_transfer_and_revision/corrnaf`) and segmented by text units (rather than sentences). 
Each file represents the historical text or editorial notes of a given letter. 
* `datasplit_all_standard` provides the train/dev/test files used for standard NER experiments (no distinction between LOC and GPE) on all the data.
 See the [ner documentation](../ner/README.md#standard-ner) to replicate this datasplit.
 
Other datasplits can be generated with the `./ner/scripts/preprocessing/prep-data.py` script combined with (a variant of) `./ner/resources/datasplit.json`.
See the [ner documentation](../ner/README.md) for more information.

## Text-Fabric annotation loop
The data consist of missives extracted from Text-Fabric, NER system output, and annotation files for Text-Fabric:
* `tf`: missives extracted from Text-Fabric (text and position files)
* `basenaf`: missives extracted from Text-Fabric in NAF format (for the record)
* `ner`: NER system output in TSV format
* `export`: annotation files for Text-Fabric ingestion

These data can be obtained with the script `./scripts/tf/ner-loop.sh`.

