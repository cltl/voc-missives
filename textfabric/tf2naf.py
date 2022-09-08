import json
import os
import sys

from utils.tfHandler import MissivesLoader
from utils.nafHandler import Naf
import utils.tfHandler as tfh

MODEL_NAME = "tf-clariah-gm"
LATEST = True


def export_letters_fromTF(workdir, text_type, max_letters=0, ml=None):
    """Export letters from TF to text and NAF
    :param workdir: the output directory for TF and NAF output
    :param ml: a MissivesLoader, possibly preloaded
    :param text_type: 'text' or 'notes'
    :param max_letters: for test, limits the number of exported letters
    """
    if ml is None:
        ml = MissivesLoader()
    tfdir, refnafdir = create_outdirs(workdir)
    if text_type == "text":
        pubids_titles_and_tunits = ml.extract_letters_text(
            tfdir, max_letters=max_letters
        )
    else:
        pubids_titles_and_tunits = ml.extract_letters_notes(
            tfdir, max_letters=max_letters
        )
    convert(pubids_titles_and_tunits, tfdir, refnafdir, ml.version())


def export_letter(workdir, v, let, text_type, ml=None):
    if ml is None:
        ml = MissivesLoader(latest=LATEST)
    tfdir, refnafdir = create_outdirs(workdir)
    pubids_titles_and_tunits = ml.extract_letter(v, let, text_type, tfdir)
    convert(pubids_titles_and_tunits, tfdir, refnafdir, ml.version())


def convert(pubids_titles_and_tunits, tfdir, refnafdir, version):
    for pubid, title, paragraphs in pubids_titles_and_tunits:
        create_naf(
            pubid, title, paragraphs, os.path.join(tfdir, pubid), refnafdir, version
        )


def create_outdirs(workdir):
    tfdir = os.path.join(workdir, "tf")
    os.makedirs(tfdir, exist_ok=True)
    refnafdir = os.path.join(workdir, "basenaf")
    os.makedirs(refnafdir, exist_ok=True)
    return tfdir, refnafdir


def create_naf(pub_id, titre, tunits, tf_text_file, nafdir, version):
    naf = Naf()
    naf.create_header(pub_id, titre)
    with open(tf_text_file) as f:
        raw = "".join(f.readlines())
        naf.add_raw_layer(raw, MODEL_NAME, version)
    naf.add_tunits(tunits, MODEL_NAME, version)
    naf.write(nafdir)


def export_letters(letters_json, outdir, ml=None):
    """exports letters specified in letters_json to outdir -- text, pos and naf files"""
    if ml is None:
        ml = MissivesLoader()
    with open(letters_json) as f:
        json_ids = json.load(f)
    letter_ids = [x["tf_id"] for x in json_ids]
    tfdir, refnafdir = create_outdirs(outdir)
    pubids_titles_and_tunits = ml.write_text_and_pos_files(letter_ids, tfdir)
    convert(pubids_titles_and_tunits, tfdir, refnafdir, ml.version())


def read_tf_files(file):
    return tfh.read_text_and_pos_files(file)


if __name__ == "__main__":
    jsonfile = sys.argv[1]
    workdir = sys.argv[2]
    export_letters(jsonfile, workdir)
