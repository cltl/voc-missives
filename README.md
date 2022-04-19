# VOC missives
This repository provides experimental code and data for Named Entity Recognition (NER) on the VOC's [General Letters](http://resources.huygens.knaw.nl/vocgeneralemissiven). 
This project is part of [Clariah WP6/Text](https://www.clariah.nl/wp6-text).

### Repository Status (April 2022)
We are currently revising the documentation and tests in view of a release. The code is not being developed anymore but issues or questions can be posted on the [Issues page](https://github.com/cltl/voc-missives/issues). 

### Background 
For this project, we used OCR texts that were first set to TEI by the [Instituut van de Nederlandse Taal](https://ivdnt.org/) and then made available through [TextFabric](https://github.com/CLARIAH/wp6-missieven). A number of missives were manually annotated by the [Huygens Institute for the History of the Netherlands](https://www.knaw.nl/en/institutes/huygens-ing), using the text extracted from the TEI set of letters; these annotations were later realigned to the TextFabric text variant. These manual annotations were then used for training a NER model with the [HuggingFace Transformers](https://huggingface.co/docs/transformers/index) library.

### Project structure
Most of the code is written in Java. We used Python to interact with TextFabric or to prepare data for NER experiments.

The repository is structured as follows:
* `./data`: manual annotations, intermediary files and NER training corpus (see [NER data](data/README.md))
* `./ner`: (Python) code and config files for [NER](ner/README.md) model training and experiments 
* `./src`: (Java) code for the conversion of the missives between different [formats](docs/formats.md), but also for manual annotation analysis
* `./scripts`: (shell) conversion, analysis and utility scripts both for Java and Python functions. 
* `./tf`: (Python) code for letter extraction from [Text-Fabric](tf/README.md)


### Citation

If you use the code or data in this repository, please cite:
```
@inproceedings{arnoult-etal-2021-batavia,
    title = "Batavia asked for advice. Pretrained language models for Named Entity Recognition in historical texts.",
    author = "Arnoult, Sophie I.  and
      Petram, Lodewijk  and
      Vossen, Piek",
    booktitle = "Proceedings of the 5th Joint SIGHUM Workshop on Computational Linguistics for Cultural Heritage, Social Sciences, Humanities and Literature",
    month = nov,
    year = "2021",
    address = "Punta Cana, Dominican Republic (online)",
    publisher = "Association for Computational Linguistics",
    url = "https://aclanthology.org/2021.latechclfl-1.3",
    pages = "21--30"
}
```

### Running a NER model
One of the models trained for this publication is available on the [HuggingFace Transformers Hub](https://huggingface.co/models) as [CLTL/gm-ner-xlmrbase](https://huggingface.co/CLTL/gm-ner-xlmrbase)
 



