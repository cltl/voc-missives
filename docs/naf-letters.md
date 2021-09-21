

## NAF layers
Input and reference NAF documents contain the following layers:

* `header`: header containing metadata and a list of layers and processors
* `raw`: raw text
* `tunits`: text units
* `text`: tokens 
* `entities`: entities 

### Header
The header contains the linguistic processors for each layer and metadata. The metadata hold:

  * the title of the letter (shared by *text* and *notes* files) 
  * a public id, identifying the volume, the index of the letter in the volume, and a text/notes identifier 
  
Each layer of annotation has a list of processors involved in creating/modifying this layer.

Example:
```xml
<nafHeader>
        <fileDesc title="De Carpentier, Dedel, Reyersz, Van Uffelen; Kasteel Jakatra, 9 juli 1621" creationtime="2021-06-23T19:22:48CEST" filename="missive_1_49_notes.naf"/>
        <public publicId="missive_1_49_notes"/>
        <linguisticProcessors layer="raw">
            <lp name="tf-clariah-gm" version="0.8.1" timestamp="2021-06-23T19:22:48CEST"/>
        </linguisticProcessors>
        ...
        <linguisticProcessors layer="entities">
            <lp name="voc-missives-man-in2naf" version="1.1" timestamp="2021-06-23T19:23:29+0200"/>
            <lp name="voc-missives-corr-conllIn2naf" version="1.1.1" timestamp="2021-07-09T17:31:41+0200"/>
        </linguisticProcessors>
    </nafHeader>
```

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
    <tunit id="t1" type="remark" xpath="//volume[1]/missive[49]/remark[1]" offset="0" length="33"/>
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

* identifier
* type (label)
* span of covered tokens.

Example:
```xml
<entity id="e0" type="PER">
    <span>
        <target id="w0"/>
    </span>
</entity>
```

