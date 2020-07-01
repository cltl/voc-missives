# VOC missives

This repository provides code for text extraction and preprocessing of the VOC missives for the *Clariah WP6* project. 

Specifically, TEI input files are converted to UIMA CAS XMI, and segmented and tokenized using a rule-based tokenizer.

## Quick start

Preprocessing relies on the ixa-pipe tokenizer `ixa-pipe-tok`, version 2.0.0. To install:

>   git clone https://github.com/ixa-ehu/ixa-pipe-tok.git
>   cd ixa-pipe-tok
>   mvn clean install

You can now clone this repository, and run:

>   mvn clean package

This will generate code for binding TEI XML elements to Java objects, as well as an executable jar for TEI-to-XMI conversion.  

**Note**: TEI-to-XMI conversion is currently set to output raw text and paragraphs only.

You can use the script `run-tei2xmi.sh` to convert files from directory to directory:

>   bash run-tei2xmi.sh [input-dir] [output-dir]
  

## XML-Java binding and code generation

The code relies on [Jaxb](https://javaee.github.io/jaxb-v2/) to map the TEI XML representation to java objects. The classes for these objects are generated at compilation with the [maven-jaxb-plugin](https://github.com/highsource/maven-jaxb2-plugin). The code can be generated (without `jar` packaging) with:

>   mvn clean compile

Code binding relies on a XSD schema for the input TEI files, and on a bindings file. The schema and bindings are located in `./src/main/resources/`. 
Binding was tested with `xjc` version 2.3.1 under Java 10, and `xjc` version 2.2.8 under Java 8.
We are using [tei_all.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all.xsd) and the related files [tei_all_dcr.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_dcr.xsd), [tei_all_teix.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_teix.xsd) and [tei_all_xml.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_xml.xsd).

## Segmentation and tokenization

Paragraphs respect the structure of the input TEI, and bear their TEI identifier.

Paragraphs are segmented into sentences and tokenized using the [ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) library.

## UIMA CAS XMI conversion

Conversion to UIMA CAS XMI is based on [DKPro core](https://dkpro.github.io/dkpro-core/).
