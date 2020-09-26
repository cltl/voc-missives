# Installation
### Requirements
The code is compatible with Java 8+. You will need maven and `xjc` to compile it.

To test whether they are installed, run
```sh
$ java -version
$ mvn -version
$ xjc -version
```

 The code was tested with `java` 1.8, `maven` 3.6.2, and `xjc` 2.2.8.

### Dependencies
Dependencies are specified in the maven `pom`, and taken care of at compilation.

The only exception is the tokenizer `ixa-pipe-tok`, which relies on a development version. To install:

```sh
$ git clone https://github.com/ixa-ehu/ixa-pipe-tok.git
$ cd ixa-pipe-tok
$ git checkout 1ac83fe
$ mvn clean install
```

### Compilation
You can now clone the *voc-missives* repository, and compile the code:

```sh
$ git clone https://github.com/cltl/voc-missives.git
$ cd voc-missives
$ mvn clean package
```

This will create an executable jar under `target`. See [Usage](usage.md) for 
scripts and options to the `jar`.