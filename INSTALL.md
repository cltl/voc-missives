# Installing
The repository consists of different modules. Which ones to install depends on your needs:

* format conversions, analysis scripts and interaction with Text-Fabric rely on a java executable that is provided in `./target/voc-missives-1.1.jar`. Installation instructions for the [Java source code](#java-source-code) are therefore only provided for reference.
* for interaction with TextFabric, see [install TextFabric](#installing-textfabric)
* to run NER experiments, [install Transformers](#installing-transformers)
* to run a fine-tuned NER model, see [install pipeline](#installing-the-ner-pipeline)

The installation script `./scripts/install.sh` collects installation commands for all modules:
```
bash ./install.sh [--java] [--textfabric] [--transformers] [--pipeline]
```

## Java source code
The code is precompiled in `./target/voc-missives-1.1.jar`, so you can directly run scripts. Follow the instructions in this section if you want to recompile.

### Requirements
The code is compatible with Java 8 (JDK). You will need `xjc` (included in the JDK) to generate some of the source code, and `maven` to compile and package the code.

To test whether they are installed, run
```sh
$ java -version
$ mvn -version
$ xjc -version
```

The code was tested with `java` 1.8, `maven` 3.6.2, and `xjc` 2.2.8.

### Dependencies and compilation
Run the installation script with the `java` flag to compile the java code and install dependencies:
```sh
bash ./install.sh --java
```

Dependencies are specified in the maven `pom`, and taken care of at compilation. The only exception is the tokenizer `ixa-pipe-tok`, which relies on a development version. 

The script performs the following operations:
```sh
# install the tokenizer
cd dependencies
git clone https://github.com/ixa-ehu/ixa-pipe-tok.git
cd ixa-pipe-tok
git checkout 1ac83fe
mvn clean install
# compile and package the voc-missives code
mvn clean package
```

## Installing TextFabric
This step can be performed with the installation script:
```sh
bash ./install.sh --textfabric
```

The script clones the TextFabric [clariah-gm](https://github.com/Dans-labs/clariah-gm) repository and the [missieven app](https://github.com/annotation/app-missieven), stores them in your home directory, and finally installs python dependencies for TextFabric and NAF (you may want to activate a *dedicated Python environment* for this).

The script performs the following operations:
```sh
# clone TextFabric repositories
git clone https://github.com/CLARIAH/wp6-missieven ~/github/CLARIAH/wp6-missieven
git clone https://github.com/Dans-labs/clariah-gm ~/github/Dans-labs
cd ~/github/Dans-labs
git checkout fc67e0b
# install python dependencies
pip install -r ${workdir}/textfabric/requirements.txt
```


## Installing Transformers
This step can be performed with the installation script:
```sh
bash ./install.sh --transformers
```
NOTE: you may want to activate a *dedicated Python environment* for this

The script performs the following operations:
```sh
cd dependencies
git clone https://github.com/huggingface/transformers
cd transformers
git checkout 626a0a0
pip install
cd examples/pytorch/token-classification
pip install -r requirements.txt
```

## Installing the NER pipeline
To run a fine-tuned model, it is sufficient to [install Transformers](https://huggingface.co/docs/transformers/installation). In your favorite environment, run

```sh
bash ./install.sh --pipeline
```