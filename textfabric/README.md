# Text-Fabric missive extraction

This directory provides scripts to extract missives from TextFabric, and in 
particular missives for which we have NE annotations. 

The Text-Fabric sources are in [CLARIAH/wp6-missieven](https://github.com/CLARIAH/wp6-missieven),
where there are several versions present.
The version used is indicated by `TF_DATA_VERSION` in [tfHandler.py](utils/tfHandler.py)

Scripts allow to   
* extract missives from TextFabric and format them to NAF (`tf2naf.py`). The file `./resources/annotated.json` specifies 
which missives to extract. Missives are identified by their volume number, their list index in the volume, and the type of 
text of the missive (historical *text* or editorial *notes*). 
* prepare annotations in tsv format for import back into TextFabric (`tsv2tf.py`)

Pipeline scripts are also provided in 
`./voc-missives/scripts/tf`. You will need to [compile the java code](../INSTALL.md#java-source-code) to use them (see 
[Formats and conversion functions](../docs/formats.md) for more information).

# Text-Fabric entity delivery

The pipeline in this repo has preserved the link between the detected entities and the Text-Fabric source.
The resulting entities have been saved as Text-Fabric features named `entityId` and `entityKind`
in the `export/tf` directory, and then under the specific version of the tf data.

## Prerequisites
See [INSTALL.md](../INSTALL.md#installing-textfabric) for instructions on installing TextFabric.

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
TextFabric comes with an extensive [tutorial](https://nbviewer.jupyter.org/github/CLARIAH/wp6-missieven/tree/master/tutorial/) for the missives.
See also the TextFabric documentation on the structure and features of the [missives in TextFabric](https://github.com/CLARIAH/wp6-missieven/blob/master/docs/transcription.md)
In particular, see the [entities tutorial](https://nbviewer.org/github/CLARIAH/wp6-missieven/blob/master/tutorial/entities.ipynb) for how to use the entity data produced here together with the original source.