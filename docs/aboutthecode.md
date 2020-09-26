# About this code
Manipulation of the XML-based formats TEI and NAF is performed through XML-Java bindings, which are automatically generated with Jaxb.

## XML-Java binding and code generation

The code relies on [Jaxb](https://javaee.github.io/jaxb-v2/) to map the TEI and NAF XML representations to java objects. The classes for these objects are generated at compilation with the [maven-jaxb-plugin](https://github.com/highsource/maven-jaxb2-plugin). The code can be generated (without `jar` packaging) with:

>   mvn clean compile

Code binding relies on a XSD schema and a bindings specification for both TEI and NAF. The schema and bindings are located in `./src/main/resources/`.
Binding was tested with `xjc` version 2.3.1 under Java 10, and `xjc` version 2.2.8 under Java 8.

## XSD schemas
### TEI
We use the TEI *All* specifications: [tei_all.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all.xsd) and the related files [tei_all_dcr.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_dcr.xsd), [tei_all_teix.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_teix.xsd) and [tei_all_xml.xsd](https://tei-c.org/release/xml/tei/custom/schema/xsd/tei_all_xml.xsd).

### NAF
The NAF xsd schema `naf_v3.1.b.xsd` is derived from a modified NAF DTD `naf_v3.1.b.dtd`, 
which extends the base [naf_v3.dtd](https://github.com/cltl/NAF-4-Development/blob/master/res/naf_development/naf_v3.dtd) with a `tunits` specification.

DTD-to-XSD conversion was performed with [trang](https://relaxng.org/jclark/trang.html). 
