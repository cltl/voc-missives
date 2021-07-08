# TextFabric missive extraction

This directory provides scripts to extract missives from TextFabric, and in 
particular missives for which we have NE annotations. 

Scripts allow to   
* extract missives from TextFabric and format them to NAF (`tf2naf.py`). The file `./resources/annotated.json` specifies 
which missives to extract. Missives are identified by their volume number, their list index in the volume, and the type of 
text of the missive (historical *text* or editorial *notes*). The `tf2naf.py` script is called by `voc-missives` scripts 
(see `./voc-missives/scripts/tf`)
* prepare annotations in tsv format for import back into TextFabric (`tsv2tf.py`)

## Prerequisites

Clone the TextFabric [clariah-gm](https://github.com/Dans-labs/clariah-gm) repository and the [missieven app](https://github.com/annotation/app-missieven):
```
git clone https://github.com/Dans-labs/clariah-gm
git clone https://github.com/annotation/app-missieven
```
And place both the `Dans-labs` and `annotation` folder under a folder `github` in your home directory:
```
mkdir -p ~/github/Dans-labs
mkdir -p ~/github/annotation
mv clariah-gm ~/github/Dans-labs
mv app-missieven ~/github/annotation
```

Install lxml and text-fabric:
```
pip install -r requirements.txt
```

## Resources
TextFabric comes with extensive [tutorials](https://nbviewer.jupyter.org/github/annotation/tutorials/tree/master/missieven/) for the missives.
See also the TextFabric documentation on the structure and features of the [missives in TextFabric](https://github.com/Dans-labs/clariah-gm/blob/master/docs/transcription.md)