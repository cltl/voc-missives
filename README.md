# VOC missives

This repository provides code for the preprocessing of the VOC missives for the *Clariah WP6* project. 

Specifically, we provide code for the conversion of TEI input files to NAF, UIMA CAS XMI, and CoNLL. Segmentation and tokenization relyon a rule-based tokenizer.

## Quick start
### Requirements
The code is compatible with Java 8+. The code was tested with maven 3.6.2.

Besides, preprocessing relies on the ixa-pipe tokenizer `ixa-pipe-tok`, version 2.0.0. To install:

```sh
$ git clone https://github.com/ixa-ehu/ixa-pipe-tok.git
$ cd ixa-pipe-tok
$ mvn clean install
```

### Installation
You can now clone this repository:

```sh
$ git clone https://github.com/cltl/voc-missives.git
$ cd voc-missives
$ mvn clean package
```

This will compile the code and create an executable jar for TEI-to-NAF conversion. 

You can use the script `./scripts/run-tei2naf.sh` to convert files from directory to directory:

>   bash scripts/run-tei2naf.sh [input-dir] [output-dir]
  
## NAF layers
Output NAF documents contain the following layers:

* Header  
* Raw layer
* Text-units layer
* [TODO] Word-forms layer
* [TODO] Entities layer
 
### Header 
The header contains the linguistic processors and metadata. The metadata hold:

  * the title of the missive as indicated by the TEI title
  * the TEI filename

### Raw layer
This contains the text extracted from TEI files. The code extracts a simplified and unified TEI tree from the input TEI, and derives an internal document representation from it, listing the `head`, `fw`, `p` and `note` TEI elements as document *paragraphs*. Note that only top-level elements are considered: if for instance a `note` is dominated by a `p` element in the TEI tree, only the `p` element will be listed.
The raw layer consists of the string yields of the *paragraph* subtrees.

The raw layer is written as CDATA in the output NAF.

### Text-units layer
The `tunits` or text-units layer can be used to represent discourse units, or text sections more generally. 
We use it here to list the document *paragraphs* (headers, forewords, paragraphs and notes) extracted from the TEI input files. Each text unit:

* is anchored to the raw layer by character offsets;
* carries the TEI identifier of its original TEI element.

### Word-forms layer
[TODO] The tokenizer is currently only part of the preprocessing for TEI-to-XMI conversion, and must be ported to NAF.

The word-forms layer is produced by the [ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) tokenizer, which performs sentence segmentation and tokenization for each text unit. Word forms have the following attributes:

* character offsets (offset and length)
* identifier
* sentence identifier (sentence number in a paragraph)
* paragraph identifier (optional here, as it can be inferred from the character offsets)

### Entities layer
[TODO] The entities layer is currently obtained by aligning manual annotations into reference XMI documents, and must be ported to NAF.

The entities layer stores named entities created either by manual annotations or by a NER system. Manual annotations are either in UIMA CAS XMI or CoNLL format, and NER annotations in CoNLL format. Entities have the following attributes:

* identifier
* span of covered tokens; we use word-form identifiers here.

## XML-Java binding and code generation

The code relies on [Jaxb](https://javaee.github.io/jaxb-v2/) to map the TEI and NAF XML representations to java objects. The classes for these objects are generated at compilation with the [maven-jaxb-plugin](https://github.com/highsource/maven-jaxb2-plugin). The code can be generated (without `jar` packaging) with:

>   mvn clean compile

Code binding relies on a XSD schema and a bindings specification for both TEI and NAF. The schema and bindings are located in `./src/main/resources/`. 
Binding was tested with `xjc` version 2.3.1 under Java 10, and `xjc` version 2.2.8 under Java 8.

### TEI
We use the TEI *All* specifications: [tei_all.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all.xsd) and the related files [tei_all_dcr.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_dcr.xsd), [tei_all_teix.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_teix.xsd) and [tei_all_xml.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_xml.xsd).

### NAF
The NAF xsd schema, `naf_v3.1.b.xsd` was derived from a modified NAF DTD, converted to XSD with [trang](https://relaxng.org/jclark/trang.html). The modified NAF DTD, `naf_v3.1.b.dtd`, is based on the [naf_v3.dtd](https://github.com/cltl/NAF-4-Development/blob/master/res/naf_development/naf_v3.dtd), and extended with a `tunits` specification.

