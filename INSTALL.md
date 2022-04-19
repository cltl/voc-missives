# Installing
The repository consists of three main modules. Which ones to install depends on your needs:

* for format conversions and analysis scripts, compile the [Java source code](#java-source-code)
* for interaction with TextFabric (to extract and format new General Letters from TextFabric, or to upload entity-annotated letters to TextFabric), compile the [Java source code](#java-source-code) and [install TextFabric](#installing-textfabric)
* to run NER experiments, [install Transformers](#installing-transformers)

The installation script `./scripts/install.sh` collects installation commands for all three modules:
```
bash ./scripts/install.sh [--java] [--textfabric] [--transformers]
```

## Java source code
### Requirements
The code is compatible with Java 8+ (JDK). You will need `xjc` (included in the JDK) to generate some of the source code, and `maven` to compile and package the code.

To test whether they are installed, run
```sh
$ java -version
$ mvn -version
$ xjc -version
```

 The code was tested with `java` 1.8, `maven` 3.6.2, and `xjc` 2.2.8.

### Dependencies and compilation
Dependencies are specified in the maven `pom`, and taken care of at compilation.

The only exception is the tokenizer `ixa-pipe-tok`, which relies on a development version. 

Running the installation script with the `java` flag will install this dependency, and then compile and package the source code:

```sh
bash ./scripts/install.sh --java
```

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
bash ./scripts/install.sh --textfabric
```

The script clones the TextFabric [clariah-gm](https://github.com/Dans-labs/clariah-gm) repository and the [missieven app](https://github.com/annotation/app-missieven), stores them in your home directory, and finally installs python dependencies for TextFabric and NAF (you may want to activate a *dedicated Python environment* for this)

The script performs the following operations:
```sh
# clone TextFabric repositories
mkdir -p ~/github/Dans-labs
mkdir -p ~/github/annotation
git clone https://github.com/Dans-labs/clariah-gm ~/github/Dans-labs
git clone https://github.com/annotation/app-missieven ~/github/annotation
# install python dependencies
pip install -r tf/requirements.txt
```

## Installing Transformers
This step can be performed with the installation script:
```sh
bash ./scripts --transformers
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
