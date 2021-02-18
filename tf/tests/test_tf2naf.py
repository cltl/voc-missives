import tf2naf as t2n
import os
import utils.nafHandler as naf
import glob
from utils.tfHandler import MissivesLoader

ml = MissivesLoader()


def test_letter_extraction():
    workdir = 'tests/data/text_1_5'
    t2n.export_letters_fromTF(workdir, "text", 5, ml=ml)
    naf_doc = naf.parse(os.path.join(workdir, 'basenaf', 'missive_1_5_text.naf'))
    tunits = naf_doc.get_tunits()
    assert tunits[0].get('xpath') == '//volume[1]/missive[5]/header[1]'
    assert int(tunits[1].get('offset')) > 0
    assert int(tunits[1].get('offset')) == int(tunits[0].get('offset')) + int(tunits[0].get('length'))


def test_notes_extraction():
    workdir = 'tests/data/notes_1_5'
    t2n.export_letters_fromTF(workdir, 'notes', 5, ml=ml)
    naf_file = os.path.join(workdir, 'basenaf', 'missive_1_5_notes.naf')
    assert os.path.exists(naf_file)
    naf_doc = naf.parse(naf_file)
    tunits = naf_doc.get_tunits()
    assert tunits[0].get('xpath') == '//volume[1]/missive[5]/footnote[1]'
    assert int(tunits[1].get('offset')) > 0
    assert int(tunits[1].get('offset')) == int(tunits[0].get('offset')) + int(tunits[0].get('length'))


def test_extract_letter_v_l_text():
    v = 5
    l = 7
    workdir = 'tests/data/letter_{}_{}'.format(v, l)
    t2n.export_letter(workdir, v, l, 'text', ml=ml)
    assert os.path.exists(os.path.join(workdir, 'tf', 'missive_{}_{}_textf'.format(v, l)))


def test_extract_letter_9_9_text():
    workdir = 'tests/data/letter_9_9'
    t2n.export_letter(workdir, 9, 9, 'text', ml=ml)
    naf_file = os.path.join(workdir, 'basenaf', 'missive_9_9_text.naf')
    assert os.path.exists(naf_file)
    naf_doc = naf.parse(naf_file)
    tunits = naf_doc.get_tunits()
    titles = [t for t in tunits if t.get('type') == 'header']
    assert len(titles) == 18


def test_export_letters_matching_annotated_files():
    workdir = 'tests/data/annotations'
    tfdir = os.path.join(workdir, 'tf')
    t2n.export_letters('resources/annotated.json', workdir)
    files = [f for f in glob.iglob('{}/**'.format('tests/data/annotations/basenaf'))]
    assert len(files) == 42
    rec = t2n.read_tf_files(os.path.join(tfdir, 'missive_1_49_notes'))
    assert rec is not None

