from lxml import etree
import time
import os


class Naf:
    def __init__(self):
        self.tree = etree.ElementTree()
        self.root = etree.Element("NAF")
        self.tree._setroot(self.root)
        self.root.set('{http://www.w3.org/XML/1998/namespace}lang', "en")
        self.root.set('version', "3.2")

    def create_header(self, public_id, title):
        # Create text and terms layers.
        naf_header = etree.SubElement(self.root, "nafHeader")
        # add fileDesc child to nafHeader
        filedesc_el = etree.SubElement(naf_header, 'fileDesc')
        timestamp = time.strftime('%Y-%m-%dT%H:%M:%S%Z')
        filedesc_el.set('creationtime', timestamp)
        if type(title) == tuple:
            title = " ".join([t for t in title])
        filedesc_el.set('title', title)
        filedesc_el.set('filename', "{}.naf".format(public_id))
        # add public child to nafHeader
        public_el = etree.SubElement(naf_header, 'public')
        public_el.set('publicId', public_id)
        return naf_header

    def add_raw_layer(self, raw_text, model_name, model_version):
        raw_layer = etree.SubElement(self.root, 'raw')
        raw_layer.text = etree.CDATA(raw_text)
        self.add_linguistic_processors_elt('raw', model_name, model_version)

    def add_linguistic_processors_elt(self, layer, model_name, model_version):
        ling_proc = etree.SubElement(self.get_layer('nafHeader'), "linguisticProcessors")
        ling_proc.set("layer", layer)
        lp = etree.SubElement(ling_proc, "lp")
        timestamp = time.strftime('%Y-%m-%dT%H:%M:%S%Z')
        lp.set("timestamp", timestamp)
        lp.set('name', model_name)
        lp.set('version', str(model_version))

    def get_layer(self, layer):
        return [x for x in self.root.iter(layer)][0]

    def get_title(self):
        return self.get_layer('fileDesc').get('title')

    def get_raw_text(self):
        return self.get_layer('raw').get('text')

    def get_public_id(self):
        naf_header = self.get_layer('nafHeader')
        return [x for x in naf_header.iter('public')][0].get('publicId')

    def get_file_name(self):
        naf_header = self.get_layer('nafHeader')
        return [x for x in naf_header.iter('fileDesc')][0].get('filename')

    def add_tunits(self, paragraphs, model_name, model_version):
        tunits_layer = etree.SubElement(self.root, 'tunits')
        for id, (xpath, offset, length, p_type) in enumerate(paragraphs, start=1):
            tunit = etree.SubElement(tunits_layer, 'tunit')
            tunit.set("xpath", xpath)
            tunit.set('type', p_type)
            tunit.set("id", f"t{id}")
            tunit.set("offset", f"{offset}")
            tunit.set("length", f"{length}")
        self.add_linguistic_processors_elt('tunits', model_name, model_version)

    def get_tunits(self):
        return [t for t in self.get_layer('tunits').iter('tunit')]

    def write(self, outdir):
        outfile = os.path.join(outdir, self.get_file_name())
        self.tree.write(outfile, encoding='utf-8', pretty_print=True, xml_declaration=True)


def parse(path):
    naf = Naf()
    naf.tree = etree.parse(path)
    naf.root = naf.tree.getroot()
    return naf
