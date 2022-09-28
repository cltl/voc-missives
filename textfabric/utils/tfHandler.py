import glob
import os
import re
from typing import Callable

from tf.app import use
from tf.core.api import Locality as L
from tf.core.api import NodeFeatures as F
from tf.convert.recorder import Recorder
from tf.fabric import Fabric as TF

# The version of the text=fabric data in CLARIAH/wp6-missieven
TF_DATA_VERSION = "1.0"


# retrieve volume, letter id and text/notes text type
TFP = re.compile(r"missive_(\d+)_(\d+)_(\w+)")

# handling punctation and spacing for raw text
WORD = re.compile(r"\w+")
DIGITS = re.compile(r"\d+")
COMP_ENDS = [
    "e",
    "8",
    "s",
    "®",
    "6",
    "en",
    "Ie",
    "le",
    "ie",
    "a",
    "as",
    "35",
    "88",
    "c",
]
S_ENDS = ["1", "1*", "‘", "ta"]
N_STARTS = ["Jansz"]
RA_STARTS = ["S", "Senh"]
R_STARTS = ["gouv", "gouvern", "Compt"]


def default_tf_loc():
    GITHUB = os.path.expanduser("~/text-fabric-data")
    ORG = "CLARIAH"
    REPO = "wp6-missieven"
    PATH = "tf"
    location = f"{GITHUB}/{ORG}/{REPO}/{PATH}"
    return location


class MissivesLoader:
    def __init__(self, latest=True, mod=None):
        silent = "deep"
        if mod is None:
            if latest:
                self.A = use(
                    "CLARIAH/wp6-missieven:clone",
                    checkout="clone",
                    version=TF_DATA_VERSION,
                    silent=silent,
                    hoist=globals(),
                )
            else:
                self.A = use(
                    "CLARIAH/wp6-missieven",
                    version=TF_DATA_VERSION,
                    silent=silent,
                    hoist=globals(),
                )
        else:
            self.A = use(
                "CLARIAH/wp6-missieven",
                version=TF_DATA_VERSION,
                mod=mod,
                silent=silent,
                hoist=globals(),
            )
        self.DEFAULT_LOC = default_tf_loc()
        self.vid_to_node = {F.n.v(vnode): vnode for vnode in F.otype.s("volume")}

    def reload(self):
        self.A = self.A.reuse()

    def version(self):
        return self.A.version

    def get_letters(self):
        letters = []
        for vnode in F.otype.s("volume"):
            vid = F.n.v(vnode)
            for lid, letter in enumerate(self.get_letters_for_volume(vid), start=1):
                letters.append((vid, lid, letter))
        return letters

    def get_letters_for_volume(self, vid):
        return L.d(self.vid_to_node[vid], otype="letter")

    def record_entities_as_tf_features(self, textdir, tsvdir, location=None):
        features = make_features(textdir, tsvdir)
        self.save_to_tf(features, location)

    def save_to_tf(self, features, location=None):
        if location is None:
            location = default_tf_loc()
        TF.save(
            nodeFeatures=features,
            metaData=get_metadata(),
            location=location,
            module=self.version(),
            silent="verbose",
        )

    def extract_letters_all(self, outdir, max_letters=0):
        return self.extract_letters(outdir, all_recorder, max_letters)

    def extract_letters_text(self, outdir, max_letters=0):
        return self.extract_letters(outdir, text_recorder, max_letters)

    def extract_letters_notes(self, outdir, max_letters=0):
        return self.extract_letters(outdir, mixed_notes_recorder, max_letters)

    def extract_letters(self, outdir, recorder, max_letters):
        for_naf = []
        i = 0
        for vid, lid, letter in self.get_letters():
            if 0 < max_letters == i:
                break
            pub_id, titre, tunits = extract_letter(letter, vid, lid, recorder, outdir)
            for_naf.append((pub_id, titre, tunits))
            i += 1
        return for_naf

    def extract_letter(self, v, let, text_type, outdir):
        letter = self.get_letter(v, let)
        if text_type == "text":
            pubid, titre, tunits = extract_letter(letter, v, let, text_recorder, outdir)
        else:
            pubid, titre, tunits = extract_letter(
                letter, v, let, mixed_notes_recorder, outdir
            )
        return [(pubid, titre, tunits)]

    def get_letter(self, v, let):
        letters = self.get_letters_for_volume(v)
        return letters[let - 1]  # letter ids are indexed from 1

    def write_text_and_pos_files(self, letter_ids, outdir):
        for_naf = []
        for letter_id in letter_ids:
            m = TFP.match(letter_id)
            v = int(m[1])
            let = int(m[2])
            letter = self.get_letter(v, let)
            if m[3] == "text":
                for_naf.append(extract_letter(letter, v, let, text_recorder, outdir))
            else:
                for_naf.append(
                    extract_letter(letter, v, let, mixed_notes_recorder, outdir)
                )
        return for_naf

    def tf_ids(self, volumes_pages_and_types):
        ids = []
        for v, p, t in volumes_pages_and_types:
            for let, letter in enumerate(self.get_letters_for_volume(v), start=1):
                if F.page.v(letter) == str(p):
                    ids.append(public_id(v, let, t))
                    break
        return ids

    def record_punct_topad(self):
        rec = Recorder()
        for _, _, letter in self.get_letters():
            for w in L.d(letter, otype="word"):
                if needs_padding(w):
                    rec.start(w)
                    rec.add("{}{}\n".format(F.trans.v(w), F.punc.v(w)))
                    rec.end(w)
        return rec


def extract_letter(letter, v, let, recorder, outdir):
    rec, tunits, pub_id = recorder(letter, v, let)
    titre = F.title.v(letter)
    outfile = os.path.join(outdir, pub_id)
    rec.write(outfile)
    return pub_id, titre, tunits


def public_id(v, let, text_type):
    return "missive_{}_{}_{}".format(v, let, text_type)


def all_recorder(letter, v, let):
    seqs = typed_sequences(letter)
    rec, tunits = record_typed_sequences(v, let, seqs, letter)
    return rec, tunits, f"missive_{v}_{let}"


def text_recorder(letter, v, let):
    text_seqs = text_sequences(letter)
    rec, tunits = record_typed_sequences(v, let, text_seqs, letter)
    return rec, tunits, public_id(v, let, "text")


def mixed_notes_recorder(letter, v, let):
    notes_and_types = mixed_note_sequences(letter)
    rec, tunits = record_typed_sequences(v, let, notes_and_types, letter)
    return rec, tunits, public_id(v, let, "notes")


def make_features(textdir, tsvdir):
    features = {"entityId": {}, "entityKind": {}}
    for file in glob.iglob("{}/**".format(textdir)):
        if "." not in file:  # looking for text file without extension
            filename = os.path.basename(file)
            tsv_file = os.path.join(tsvdir, "{}.tsv".format(filename))
            file_feats = get_file_features(file, tsv_file)
            if file_feats:
                features["entityKind"].update(file_feats["entityKind"])
                features["entityId"].update(file_feats["entityId"])
    return features


def read_text_and_pos_files(text_path):
    rec = Recorder()
    rec.read(text_path)
    return rec


def get_file_features(text_path, tsv_path):
    rec = read_text_and_pos_files(text_path)
    return rec.makeFeatures(tsv_path)


def get_parent(w):
    p = L.u(w, otype="para")
    if p:
        return p[0]
    p = L.u(w, otype="remark")
    if p:
        return p[0]
    p = L.u(w, otype="footnote")
    if p:
        return p[0]
    return None


def record_section(rec, section, offset):
    pnode = get_parent(section[0])
    if pnode is not None:
        rec.start(pnode)
    for w in section:
        tok = F.trans.v(w)
        punct = F.punc.v(w)
        wordform = "{}{}".format(tok, punct)
        if wordform.strip():
            rec.start(w)
            rec.add(wordform)
            offset += len(tok) + len(punct)
            rec.end(w)
            offset = pad(offset, rec, w)
    rec.add("\n")  # end of title
    offset += 1
    if pnode is not None:
        rec.end(pnode)
    return offset


def needs_padding(w):
    """needs padding if:
    - after commas
    - after periods following on letters (not digits), and with a few exceptions
    - after empty punctuation"""
    punct = F.punc.v(w)
    token = F.trans.v(w)
    punct_ends_word = (
        punct == "."
        and WORD.match(token)
        and not DIGITS.match(token)
        and not inword_period(w)
    )
    word_ends_at_eol = (
        punct == "" and token and token[-1] != "¬"
    )  # token can be empty but never together with punct
    text_comma = punct == ","
    if L.n(w, otype="word"):
        next_word = F.trans.v(L.n(w, otype="word")[0])
        text_comma = text_comma and not (DIGITS.match(next_word) or next_word == "—")

    return word_ends_at_eol or text_comma or punct_ends_word


def pad(offset, rec, w):
    """add whitespace if needed"""
    if needs_padding(w):
        rec.add(" ")
        offset += 1
    return offset


def inword_period(w):
    token = F.trans.v(w)
    next_token = F.trans.v(L.n(w, otype="word")[0])
    return (
        token == "Comp"
        and next_token in COMP_ENDS
        or token == "S"
        and next_token in S_ENDS
        or token in R_STARTS
        and next_token == "r"
        or token == "R"
        and next_token == "1"
        or token in RA_STARTS
        and next_token == "ra"
        or token == "V"
        and next_token == "O"
        or token == "O"
        and next_token in ["C", "G", "I"]
        or token == "I"
        and next_token == "C"
        or token == "Eng"
        and next_token == "8e"
        or token == "Gomp"
        and next_token == "es"
        or token in N_STARTS
        and next_token == "n"
        or token == "i"
        and next_token == "p"
        or token == "p"
        and next_token == "v"
        or token == "t"
        and next_token == "w"
        or token == "w"
        and next_token == "v"
    )


def xpath_title(volume_id, letter_id):
    return "//volume[{}]/missive[{}]/title".format(volume_id, letter_id)


def xpath(volume_id, letter_id, section, section_id):
    return "//volume[{}]/missive[{}]/{}[{}]".format(
        volume_id, letter_id, section, section_id
    )


def tunit_info(volume_id, letter_id, section_type, unit_offset, offset, section_id=0):
    if not section_id:
        xp = "//volume[{}]/missive[{}]/{}".format(volume_id, letter_id, section_type)
    else:
        xp = xpath(volume_id, letter_id, section_type, section_id)
    return xp, unit_offset, offset - unit_offset, section_type


def record_typed_sequences(volume_id, letter_id, typed_seqs, letter):
    """records text and returns tunit info and recorder;
    remarks and footnotes are mixed and appear in order of appearance, i.e. footnotes are ordered by their mark
    :param letter:"""
    rec = Recorder()
    rec.start(letter)
    offset = 0
    t_off = 0
    tunits = []
    for n_id, (n, t) in enumerate(typed_seqs, start=1):
        offset = record_section(rec, n, offset)
        tunits.append(tunit_info(volume_id, letter_id, t, t_off, offset, n_id))
        t_off = offset
    rec.end(letter)
    return rec, tunits


def is_remark_or_footnote(word_type):
    return word_type == "r" or word_type == "n"


def starts_new_sequence(word_type, previous_type):
    return word_type != previous_type


def title(letter):
    """title words are the first part of the original text that do not belong to a paragraph"""
    title_seq = []
    for w in L.d(letter, otype="word"):
        para = L.u(w, otype="para")
        if F.isorig.v(w) and not para:
            title_seq.append(w)
        if para:
            break
    return title_seq


def trans(word_seq):
    return "".join(["{}{}".format(F.trans.v(w), F.punc.v(w)) for w in word_seq])


def list_paragraphs(letter):
    return [L.d(p, otype="word") for p in L.d(letter, otype="para")]


def is_inparagraph(w):
    return L.u(w, otype="para")


def text_sequences(letter):
    return [(seq, tseq) for (seq, tseq) in typed_sequences(letter) if tseq in ["paragraph", "header"]]


def mixed_note_sequences(letter):
    return [(seq, tseq) for (seq, tseq) in typed_sequences(letter) if tseq in ["footnote", "remark"]]


def append_remark(words, seqs, types):
    return append_unit(seqs, types, words, F.isremark.v, "remark")


def append_header(words, seqs, types):
    return append_unit(seqs, types, words, lambda w: not is_inparagraph(w), "header")


def append_note(words, notes, types=[]):
    # call with types=[] for separate processing of type sequence
    return append_unit(notes, types, words, F.isnote.v, "footnote")


def append_unit(seqs, types, words, condition: Callable[[int], bool], unit_type: str):
    i = 0
    while i < len(words) and condition(words[i]):
        i += 1
    seqs.append(words[0:i])
    types.append(unit_type)
    return i


def append_text(words, text):
    i = 0
    while i < len(words) and F.isorig.v(words[i]):
        i += 1
    text.extend(words[0:i])
    return i


def append_paragraph_and_notes(seqs, types, words):
    """Handle words contained in a paragraph. This includes the paragraph text proper and possible footnotes."""
    notes = []
    text = []
    i = 0
    paragraph = L.u(words[i], otype="para")
    words_in_paragraph = L.d(paragraph[0], otype="word")
    paragraph_length = len(words_in_paragraph)
    while i < paragraph_length:
        if F.isorig.v(words_in_paragraph[i]):
            i += append_text(words_in_paragraph[i:], text)
        elif F.isnote.v(words_in_paragraph[i]) or F.isremark.v(words_in_paragraph[i]):
            # embedded remarks are treated like notes
            i += append_note(words_in_paragraph[i:], notes)
        elif is_empty(words_in_paragraph[i]) or F.isfolio.v(words_in_paragraph[i]):
            i += 1
    seqs.append(text)
    types.append("paragraph")
    seqs.extend(notes)
    types.extend(["footnote"] * len(notes))
    return paragraph_length


def is_empty(w):
    return not F.trans.v(w) and not F.punc.v(w)


def typed_sequences(letter):
    """extract sequences of words of a same type: header, paragraph, footnote or remark.

    Words are classified as `orig`, `note` (for footnotes) or `remark`, where `orig` words are either part of a
    paragraph or not. In the latter case, they must be part of a header.
    Footnote words are embedded in paragraphs, so the extracted sequences are listed after the corresponding paragraph
    sequence.
    Sequence boundaries are marked by the change to another type of sequence, so contiguous footnotes or remarks would
    be listed in the same sequence."""
    seqs = []
    types = []
    words = L.d(letter, otype="word")
    i = 0
    while i < len(words):
        if F.isremark.v(words[i]):
            i += append_remark(words[i:], seqs, types)
        elif F.isorig.v(words[i]):
            if is_inparagraph(words[i]):
                i += append_paragraph_and_notes(seqs, types, words[i:])
            else:
                i += append_header(words[i:], seqs, types)
        elif F.isnote.v(words[i]):
            i += append_note(words[i:], seqs, types)
        elif is_empty(words[i]) or F.isfolio.v(words[i]):
            i += 1
        else:
            raise ValueError(f"Expected word to be of type orig or remark")
    return zip(seqs, types)


def get_metadata():
    metaData = {
        "entityId": dict(
            valueType="str",
            description="identifier of a named entity",
            creator="Sophie Arnoult",
        ),
        "entityKind": dict(
            valueType="str",
            description="kind of a named entity",
            creator="Sophie Arnoult",
        ),
    }
    return metaData
