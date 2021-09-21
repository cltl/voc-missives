# Usage
## Scripts
The `./scripts` folder contains scripts for the different conversion operations.
Options for running the jar are detailed below. See also [Functions](functions.md)
   for more information on the different operations.

## Command-line arguments for java functions
The different conversion and file transformation operations are triggered by the
type of input, output and (when applicable) reference files.
Input and reference file types are inferred from file extensions, unless otherwise specified (with flags `I`, `R`, `O` below).


| Argument type | Argument  | description |
| :--------:    | --------- | ----------- |
| Operation selection |   -i      | input file or directory |
|               |   -r      | reference file or directory |
|               |   -o      | output directory |
| File type overriding (file type is inferred from file extensions by default) |  -I      | input file type (`tei`, `naf`, `xmi` or `conll`) |
|               | -R      | reference file type (`naf`) |
|               | -O      | output file type (`naf` or `conll`)|
| Other options | -d    | document-type selection for `naf-selector`: 'text', 'note', or 'all' (default: 'all') |
|               | -t    | tokenize (add text layer) NAF (default: false) |
|               | -e    | source of input entities (man|corr|sys) for addition/replacement of entities layer   |
|               | -v    | version input data for NafConllReader |
|               | -w    | replace tokens with those of Conll input for NafConllReader |
|               | -n    | add new entities to existing entities for NafConllReader |
|               | -f    | format TSV for TextFabric |
|               | -u    | segment output conll by text units (instead of sentences) |
|               | -a    | analysis mode (manual|agreement) -- manual: entity statistics |
        

