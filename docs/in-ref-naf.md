# Input and Reference NAF files 

The code generates and manipulates two kinds of NAF files for the VOC use case (for TEI input):

* Input NAF files: these are obtained by conversion from TEI files. These files contain a `raw` text layer, and a `tunits` text-unit layer.
* Derived ('reference') NAF files: these are derived from the input NAF files for further NLP processing. These files contain a 
selection of the input raw text and of the text units, both of which point back to character offsets and xpath in the 
input files. Additionally, the derived NAF files contain a `text` layer for tokens, and an `entities` layer after the 
addition of entities from manual annotations or system output.

Both types of files share a number of layers, but their content differ as explained [below](#differences-between-input-and-reference-naf-layers).

## NAF layers
Input and reference NAF documents contain the following layers:

* `header`: header containing metadata and a list of layers and processors
* `raw`: raw text
* `tunits`: text units
* `text`: tokens (reference NAF only)
* `entities`: entities (reference NAF)

### Header
The header contains the linguistic processors for each layer and metadata. The metadata hold:

  * the title of the missive as indicated by the TEI title
  * the TEI filename
  * a public URI

### Raw layer
The raw layer contains the reference text (written as CDATA) for the document. Tokens are referenced by character offsets in the raw text.

Example:
```xml
<raw><![CDATA[Reniers, Maetsuyker, Demmer, Hartzinck, Van Oudtshoorn, enz.]]></raw>
```

### Text-units layer
The `tunits` or text-units layer is used to represent and list discourse units or text sections.
Each text unit:

* is anchored to the raw layer by character offsets;
* carries the TEI identifier of its original TEI element;
* carries an xpath identifying its location in the original TEI document;
* carries a type corresponding to its original TEI element.

Both the xpath and type are derived from the TEI identifier.

Example:
```xml
<tunits>
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1"
        type="text" xpath="/TEI/text[1]" offset="0" length="1259"/>
    ...        
</tunits>
```

### Text layer
The text layer lists tokens ('word forms'). 
The text layer is produced by the [ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok) tokenizer, which performs 
sentence segmentation and tokenization for each text unit. 

Word forms have the following attributes:

* character offsets (offset and length)
* identifier
* sentence identifier (sentence number in a paragraph)
* paragraph identifier 

Example:
```xml
<text>
    <wf id="w0" sent="0" para="0" offset="0" length="7">Reniers</wf>
    <wf id="w1" sent="0" para="0" offset="7" length="1">,</wf>
    <wf id="w2" sent="0" para="0" offset="9" length="10">Maetsuyker</wf>
    ...
</text>
```

### Entities layer
The entities layer is obtained by integration of manual annotations or NER system output into reference NAF documents.

Entities have the following attributes:

* identifier;
* span of covered tokens.

Example:
```xml
<entity id="e0" type="PER">
    <references>
        <span>
            <target id="w0"/>
        </span>
    </references>
</entity>
```

## Differences between input and reference NAF layers

### Header
The linguistic-processors list of the reference NAF is extended with a reference to `naf-selector` for all layers.

Besides, the filename is extended with `-text`, `-notes` or , `-all` depending on the document-type selection.

### Raw layer
#### Input NAF
The raw text contains the yield of the `<text>` element of the input TEI.

#### Reference NAF
The raw text is a selection of the input raw text, based on selection of `tunit` elements from the input TEI.
Three selections can be made:

* `text`: this excludes `note` and `fw` tunits
* `notes`: this selects `note` tunits, excluding embedded `fw` tunits
* `all`: this keeps the same raw text as the input NAF's   

### Tunits layer
#### Input NAF
Tunits correspond one-to-one to TEI elements. Consequently:
 
* they contain zero-length elements (line breaks `<lb>` and page breaks `<pb>`), as well as text-formatting elements (highlighting `<hi>`). 
Zero-length elements are given the character offset of the following character (if they appear at the end of a document, 
their character offset will consequently *fall off* the document); 
* they are structured hierarchically (as a tree), and listed in prefix order.

#### Reference NAF
Tunits are filtered and adapted for NLP processing (notably tokenization):

* zero-length and text-formatting tunits are removed; 
* tunits are flattened to form a list of non-overlapping tunits; 
* additionally, a selection of notes or text may be performed on the tunits. As this alters the raw text,  
the character offsets of flattened tunit fragments are
appended to the tunit `id` to provide a reference to the input NAF raw text. 

In the following example, tunits have been flattened, resulting in two separate fragments for the paragraph `(..).p.2`.
The character offsets of each tunit are appended to its `id` to keep a reference to the input NAF raw text.
```xml
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.co419-230"
        type="p" xpath="/TEI/text[1]/body[1]/div[1]/p[2]"
        offset="419" length="230"/>
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.note.1.co649-25"
        type="note" xpath="/TEI/text[1]/body[1]/div[1]/p[2]/note[1]"
        offset="649" length="25"/>
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.note.2.co674-47"
        type="note" xpath="/TEI/text[1]/body[1]/div[1]/p[2]/note[2]"
        offset="674" length="47"/>
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.fw.1.co721-512"
        type="fw" xpath="/TEI/text[1]/body[1]/div[1]/p[2]/fw[1]"
        offset="721" length="512"/>
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.co1233-26"
        type="p" xpath="/TEI/text[1]/body[1]/div[1]/p[2]"
        offset="1233" length="26"/>
```

Selecting notes leads to a reduced list of tunits, and a shorter raw text; character offsets change as a result, but the 
original offsets are preserved in the tunit `id`:

```xml
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.note.1.co649-25"
        type="note" xpath="/TEI/text[1]/body[1]/div[1]/p[2]/note[1]"
        offset="154" length="25"/>
    <tunit
        id="INT_0aff566f-8c02-332d-971d-eb572c33f86b.TEI.1.text.1.body.1.div.1.p.2.note.2.co674-47"
        type="note" xpath="/TEI/text[1]/body[1]/div[1]/p[2]/note[2]"
        offset="179" length="47"/>
```