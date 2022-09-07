# TextFabric missive extraction

This directory provides scripts to extract missives from TextFabric, and in 
particular missives for which we have NE annotations. 

Scripts allow to   
* extract missives from TextFabric and format them to NAF (`tf2naf.py`). The file `./resources/annotated.json` specifies 
which missives to extract. Missives are identified by their volume number, their list index in the volume, and the type of 
text of the missive (historical *text* or editorial *notes*). 
* prepare annotations in tsv format for import back into TextFabric (`tsv2tf.py`)

Pipeline scripts are also provided in 
`./voc-missives/scripts/tf`. You will need to [compile the java code](../docs/install.md) to use them (see 
[Formats and conversion functions](../docs/formats.md) for more information).

## Prerequisites

Clone the TextFabric [wp6-missieven](https://github.com/CLARIAH/wp6-missieven) repository:
```
git clone https://github.com/CLARIAH/wp6-missieven
```
And place it in a folder `CLARIAH` in folder `github` in your home directory:
```
mkdir -p ~/github/CLARIAH
mv wp6-missieven ~/github/CLARIAH
```

Install lxml and text-fabric:
```
pip install -r requirements.txt
```

## Resources
TextFabric comes with extensive [tutorials](https://nbviewer.jupyter.org/github/CLARIAH/wp6-missieven/tree/master/tutorials/) for the missives.
See also the TextFabric documentation on the structure and features of the [missives in TextFabric](https://github.com/CLARIAH/wp6-missieven/blob/master/docs/transcription.md)
