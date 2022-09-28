from collections import defaultdict

from textfabric.utils.tfHandler import MissivesLoader
import textfabric.utils.tfHandler as tfh

ml = MissivesLoader()
letters = ml.get_letters_for_volume(1)


def test_original_text_extraction():
    title = tfh.title(letters[4])
    assert tfh.trans(title) == "V. PIETER BOTH, AAN BOORD VAN DE VERE, VOOR MALEYO 31 maart 1612. "
    paras = tfh.list_paragraphs(letters[4])
    assert len(paras) == 4
    typed_paras = list(tfh.text_sequences(letters[4]))
    assert len(typed_paras) == 5


def test_extraction():
    typed_units = list(tfh.typed_sequences(letters[4]))
    groups = defaultdict(int)
    for (seq, seq_type) in typed_units:
        groups[seq_type] += 1
    assert groups["header"] == 1
    assert groups["paragraph"] == 4
    assert groups["footnote"] == 4
    assert groups["remark"] == 2


