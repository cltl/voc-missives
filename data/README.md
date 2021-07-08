# missives data

## Manual annotations
Most annotations were performed on raw text and saved as XMI files. 
One missive was first processed with an early NER system, corrected by the annotators, and then saved as Conll. 
 
## TextFabric transfer and revision 
The subfolders in this directory were obtained in this order:

* `tf` missives text extracted from TF with node ids (`.pos` files)
* `basenaf` formatting into NAF (raw text and text units)
* `toknaf` tokenization (+ tokens `text` layer)
* `mannaf` manual entity annotations (from `.data/manual/annotations`) transfered to TextFabric raw text and IXA tokens (+ `entities` layer)
* `corrconll` entity correction in conll files derived from mannaf 
* `corrnaf` entity layer in `mannaf` files replaced by corrected entities from `corrconll`

## NER
* `corpus` conll files derived from `corrnaf` (`./data/tf_transfer_and_revision/corrnaf`), segmented by text units (instead of sentences)