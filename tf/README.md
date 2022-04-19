# TextFabric missive extraction

This directory provides scripts to extract missives from TextFabric, and in 
particular missives for which we have NE annotations. 

Scripts allow to   
* extract missives from TextFabric and format them to NAF (`tf2naf.py`). The file `./resources/annotated.json` specifies 
which missives to extract. Missives are identified by their volume number, their list index in the volume, and the type of 
text of the missive (historical *text* or editorial *notes*). 
* prepare annotations in tsv format for import back into TextFabric (`tsv2tf.py`)

Pipeline scripts are also provided in 
`./voc-missives/scripts/tf`. You will need to [compile the java code](../INSTALL.md#java-source-code) to use them (see 
[Formats and conversion functions](../docs/formats.md) for more information).

## Prerequisites
See [INSTALL.md](../INSTALL.md#installing-textfabric) for instructions on installing TextFabric.

## Resources
TextFabric comes with extensive [tutorials](https://nbviewer.jupyter.org/github/annotation/tutorials/tree/master/missieven/) for the missives.
See also the TextFabric documentation on the structure and features of the [missives in TextFabric](https://github.com/Dans-labs/clariah-gm/blob/master/docs/transcription.md)