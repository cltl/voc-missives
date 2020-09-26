# VOC missives

This repository provides code for the preprocessing of the VOC missives 
for **Clariah WP6**.

Preprocessing involves converting TEI input missives to the NAF NLP format,
and handling the integration of manual and system NER annotations. 

## Overview
The code offers the following functions:
 
 * `tei2naf`: conversion of missives in TEI format to input NAF files
 * `naf-selector`: derivation of reference NAF files for NLP processing 
 by selection of *text* or *note* text units from input NAF files
 * `xmi-in2naf`: integration of manually-annotated entities in XMI format 
 into reference NAF files 
 * `naf2conll`: conversion of reference NAF files to Conll
 * `conll-in2naf`: integration of NER entities in Conll format into reference 
 NAF files
 
 ![](docs/img/clariah-doc-functionality.png)

See [Functions](docs/functions.md) for a description of each function. 

NAF is used as an internal format throughout, but we distinguish *input* NAF files, which closely follow the structure 
of the TEI missives, from *reference* NAF files that are oriented towards NLP processing.
See [Input and Reference NAF files](docs/in-ref-naf.md) for more information.


## Usage

See the [Installation](docs/install.md) instructions to compile the code and build an executable jar for the project.
This jar appears as `voc-missives-*.jar` in `./target`.

With the executable jar, you can use the scripts located in `./scripts` to run the different functions.
The scripts and command-line arguments to the `jar` are documented in [Usage](docs/usage.md).





