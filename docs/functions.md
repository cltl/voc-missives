# Functions

File conversion functions are presented here in general, see [Usage](usage.md) for running instructions.

## tei2naf
The conversion of TEI missives into *input* NAF files 
serves to extract project-input files for NLP processing.

The conversion extracts the `<text>` element from TEI files and produces a
NAF file containing the following layers:

* `raw`: the raw text contained in the `<text>` element
* `tunits`: text units corresponding to the `<text>` element and all its sub-elements. 
The text units follow the hierarchical structure of the TEI elements. 



## man-in2naf
This function inputs manual named-entity annotations in XMI or Conll format, matches them to reference NAF files, and 
enriches the NAF files with an entities layer.

Conll input files are internally converted to UIMA CAS to be processed like XMI files. The raw text is built by joining 
all Conll tokens by a single space, disregarding sentence separation.

The function performs the following operations:
* the raw text in the UIMA CAS and the NAF are compared. If they do not match, 
a heuristic search is performed to align input entity mentions with NAF tokens. 
The character offsets of the input entities are then mapped to those of their aligned mention in the reference raw text.
* entities are mapped to the reference tokens that overlap with their character offsets.
* the reference NAF is enriched with an *entities* layer, which receives the mapped entities.


## naf2conll
The conversion of reference NAF files to Conll simply:
 
* lists tokens (with their sentence identifier) and entities in the NAF file
* derives token-level entity labels 
* prints out each token with its label, separating sentences

The output format is *CONLL 2002*, where tokens and entities are separated by a single space.
 The sequence unit can be text units (`./scripts/naf2conll-textunits.sh`) or sentences (`./scripts/naf2conll-sentences.sh`).

## corr-in2naf/sys-in2naf
The integration of NER entities in Conll format into reference NAF files performs the following operations:

* tokens in the Conll and NAF files are matched to retrieve token identifiers
* token-level (BIO) NER labels for a same entity are joined
* the reference NAF is enriched with an entities layer (the operation is destructive, as we assume for now that NAF files 
need only be associated with a single, *best* set of entities), and receives entities and their token spans.