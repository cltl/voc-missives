from utils.tfHandler import MissivesLoader
import utils.tfHandler as tfh

ml = MissivesLoader()
letters = ml.get_letters_for_volume(1)


def test_note_sequence_extraction():
    remarks = tfh.list_remarks(letters[4])
    assert len(remarks) == 3
    notes = tfh.list_footnotes(letters[4])
    assert len(notes) == 5
    remarks, notes = tfh.note_sequences(letters[4])
    assert len(notes) == 5


def test_original_text_extraction():
    title = tfh.title(letters[4])
    assert tfh.trans(title) == "V. PIETER BOTH, AAN BOORD VAN DE VERE, VOOR MALEYO 31 maart 1612."
    paras = tfh.list_paragraphs(letters[4])
    assert len(paras) == 4
    title, paras = tfh.text_sequences(letters[4])
    assert len(paras) == 4


