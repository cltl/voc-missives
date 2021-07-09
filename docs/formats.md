# Formats and conversion functions
The code offers a variety of conversion scripts for different stages of processing in the missives. 
 

 ![](docs/img/clariah-doc-functionality.png)
 
We use two kinds of sources for the missives text: TEI input missives, and their adaptation to [TextFabric](https://github.com/Dans-labs/clariah-gm).
Besides, the code allows for the integration of manual entity annotations and their preparation for NER system training. 
 
  * `tei2naf`: conversion of missives in TEI format to input NAF files
  * `naf-selector`: derivation of reference NAF files for NLP processing 
  by selection of *text* or *note* text units from input NAF files (See [Input and Reference NAF files](docs/in-ref-naf.md) 
  for more information)
  * `tf2naf`: extraction of missives from TextFabric
  * `man-in2naf`: integration of manually-annotated entities in XMI or Conll format 
  into reference NAF files 
  * `corr-in2naf`: integration of corrected entities
  * `naf2conll`: conversion of reference NAF files to Conll
  * `sys-in2naf`: integration of system NER entities in Conll format into reference 
  NAF files

See also [Functions](docs/functions.md) for more information. 

Most functions are implemented in Java, see the [Installation](docs/install.md) instructions to compile the code 
and build an executable jar for the project.
For the TextFabric related functions, you will also need to install TextFabric (see [tf/README.md](tf/README.md)).

With the executable jar, you can use the scripts located in `./scripts` to run the different functions.
The scripts and command-line arguments to the `jar` are documented in [Usage](docs/usage.md).
