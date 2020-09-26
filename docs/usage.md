# Usage
## Scripts
The `scripts/` folder contains scripts for the different conversion operations.
Adapt the name of the `jar` file if necessary.
Options for the running the `jar` are detailed below. See also [Functions](functions.md)
   for more information on the different operations.

## Command-line arguments
The different conversion and file transformation operations are triggered by the
type of input, output and (when applicable) reference files.
Input and reference file types are inferred from file extensions, unless otherwise specified.

### Argument summary

| Argument type | Argument  | description |
| :--------:    | --------- | ----------- |
| Operation selection |   -i      | input file or directory |
|  |   -r      | reference file or directory |
|  |   -o      | output directory |
| File type overriding (file type is inferred from file extensions by default) |  -I      | input file type |
|  | -R      | reference file type |
|  | -O      | output file type |
| Other options | -d | document-type selection for `naf-selector`: 'text', 'note', or 'all' (default: 'all') |
| | -c | conll column separator for `naf2conll` (default: `\t`)| 

The scripts and command-line arguments for the different operations are presented below. See [Functions](functions.md)
for more information on the different operations.

### tei2naf

```jshelllanguage
java -jar voc-missives*.jar -i INPUT -I tei -o OUTPUT_DIR
```

* `-i INPUT`: this can be a file or an input directory 
* `-I tei`: the input TEI files for the project carry an `.xml` extension, we use `-I` to interpret the input as TEI. 
* `-o OUTPUT_DIR`: `tei2naf` is the only operation with TEI input. The output NAF files are written to `OUTPUT_DIR`, with 
a filename based on the (TEI) file ID.

see `./scripts/tei2naf.sh`

### naf-selector

```jshelllanguage
java -jar voc-missives*.jar -i INPUT -O naf -d DOCUMENT_TYPE -o OUTPUT_DIR
```

* `-i INPUT`: input file or directory with NAF files
* `-O naf`: there are several conversion operations from NAF, this selects `naf-selector`
* `-d DOCUMENT_TYPE`: can be `text`, `notes` or `all` (`all` by default)
* `-o OUTPUT_DIR`: output directory. File names are extended with their document type

see `./scripts/naf-selector.sh`

### xmi-in2naf
 
```jshelllanguage
java -jar voc-missives*.jar -i INPUT -r REFERENCE -o OUTPUT_DIR
```

* `-i INPUT`: input file or directory with XMI files (`.xmi` extension)
* `-r REFERENCE`: reference NAF files. XMI and NAF files names should match (`.xmi`/`.naf` extension excepted)
* `-o OUTPUT_DIR`: output directory. Output files are in NAF format with the same name as the reference.

see `./scripts/in2naf.sh`

### naf2conll

```jshelllanguage
java -jar voc-missives*.jar -i INPUT -O conll -o OUTPUT_DIR
```

* `-i INPUT`: input file or directory with NAF files (`.naf` extension)
* `-O conll`: selects `naf2conll` conversion
* `-o OUTPUT_DIR`: output directory. Output files are in NAF format with the same name as the reference.

see `./scripts/naf2conll.sh`

### conll-in2naf

```jshelllanguage
java -jar voc-missives*.jar -i INPUT -r REFERENCE -o OUTPUT_DIR
```

* `-i INPUT`: input file or directory with CONLL files (`.conll` extension)
* `-r REFERENCE`: reference NAF files. CONLL and NAF files names should match (`.conll`/`.naf` extension excepted)
* `-o OUTPUT_DIR`: output directory. Output files are in NAF format with the same name as the reference.

see `./scripts/in2naf.sh`
