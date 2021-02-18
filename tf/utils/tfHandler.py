import glob
import os
import re
from tf.app import use
from tf.core.api import Locality as L
from tf.core.api import NodeFeatures as F
from tf.convert.recorder import Recorder
from tf.fabric import Fabric as TF

# retrieve volume, letter id and text/notes text type
TFP = re.compile(r"missive_(\d+)_(\d+)_(\w+)")

# handling punctation and spacing for raw text
WORD = re.compile(r"\w+")
DIGITS = re.compile(r"\d+")
COMP_ENDS = ['e', '8', 's', '®', '6', 'en', 'Ie', 'le', 'ie', 'a', 'as', '35', '88', 'c']
S_ENDS = ['1', '1*', '‘', 'ta']
N_STARTS = ['Jansz']
RA_STARTS = ['S', 'Senh']
R_STARTS = ['gouv', 'gouvern', 'Compt']


class MissivesLoader:
    def __init__(self, mod=None):
        if mod is None:
            self.A = use('missieven', hoist=globals())
        else:
            self.A = use('missieven', mod=mod, hoist=globals())
        self.DEFAULT_LOC = default_tf_loc()

    def reload(self):
        self.A = self.A.reuse()

    def version(self):
        return self.A.version

    def get_letters(self):
        letters = []
        for vid in range(1, 14):
            for lid, letter in enumerate(self.get_letters_for_volume(vid), start=1):
                letters.append((vid, lid, letter))
        return letters

    def get_letters_for_volume(self, volume):
        if type(volume) == int:
            volume = str(volume)
        vol = self.A.nodeFromSectionStr(volume)
        return L.d(vol, otype='letter')

    def record_entities_as_tf_features(self, textdir, tsvdir, location=None):
        features = make_features(textdir, tsvdir)
        self.save_to_tf(features, location)

    def save_to_tf(self, features, location=None):
        VERSION = self.A.version
        if location is None:
            location = default_tf_loc()
        TF.save(nodeFeatures=features, metaData=get_metadata(), location=location, module=VERSION)

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

    def extract_letter(self, v, l, text_type, outdir):
        letter = self.get_letter(v, l)
        if text_type == 'text':
            pubid, titre, tunits = extract_letter(letter, v, l, text_recorder, outdir)
        else:
            pubid, titre, tunits = extract_letter(letter, v, l, mixed_notes_recorder, outdir)
        return [(pubid, titre, tunits)]

    def get_letter(self, v, l):
        letters = self.get_letters_for_volume(v)
        return letters[l - 1]  # letter ids are indexed from 1

    def write_text_and_pos_files(self, letter_ids, outdir):
        for_naf = []
        for letter_id in letter_ids:
            m = TFP.match(letter_id)
            v = int(m[1])
            l = int(m[2])
            letter = self.get_letter(v, l)
            if m[3] == 'text':
                for_naf.append(extract_letter(letter, v, l, text_recorder, outdir))
            else:
                for_naf.append(extract_letter(letter, v, l, mixed_notes_recorder, outdir))
        return for_naf

    def tf_ids(self, volumes_pages_and_types):
        ids = []
        for v, p, t in volumes_pages_and_types:
            for l, letter in enumerate(self.get_letters_for_volume(v), start=1):
                if F.page.v(letter) == str(p):
                    ids.append(public_id(v, l, t))
                    break
        return ids


def extract_letter(letter, v, l, recorder, outdir):
    rec, tunits, pub_id = recorder(letter, v, l)
    titre = L.d(letter, otype='title')
    outfile = os.path.join(outdir, pub_id)
    rec.write(outfile)
    return pub_id, titre, tunits


def public_id(v, l, text_type):
    return "missive_{}_{}_{}".format(v, l, text_type)


def text_recorder(letter, v, l):
    text_seqs = text_sequences(letter)
    rec, tunits = record_typed_sequences(v, l, text_seqs, letter)
    return rec, tunits, public_id(v, l, 'text')


def mixed_notes_recorder(letter, v, l):
    notes_and_types = mixed_note_sequences(letter)
    rec, tunits = record_typed_sequences(v, l, notes_and_types, letter)
    return rec, tunits, public_id(v, l, 'notes')


def default_tf_loc():
    GITHUB = os.path.expanduser('~/text-fabric-data')
    ORG = 'Dans-labs'
    REPO = 'clariah-gm'
    PATH = 'tf'
    location = f'{GITHUB}/{ORG}/{REPO}/{PATH}'
    return location


def make_features(textdir, tsvdir):
    features = {'entityId': {}, 'entityKind': {}}
    for file in glob.iglob("{}/**".format(textdir)):
        if '.' not in file:  # looking for text file without extension
            filename = os.path.basename(file)
            tsv_file = os.path.join(tsvdir, "{}.tsv".format(filename))
            file_feats = get_file_features(file, tsv_file)
            features['entityKind'].update(file_feats['entityKind'])
            features['entityId'].update(file_feats['entityId'])
    return features


def read_text_and_pos_files(text_path):
    rec = Recorder()
    rec.read(text_path)
    return rec


def get_file_features(text_path, tsv_path):
    rec = read_text_and_pos_files(text_path)
    return rec.makeFeatures(tsv_path)


def note_sequences(letter):
    return list_remarks(letter), list_footnotes(letter)


def get_parent(w):
    p = L.u(w, otype='para')
    if p:
        return p[0]
    p = L.u(w, otype='remark')
    if p:
        return p[0]
    p = L.u(w, otype='footnote')
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
            offset = pad(offset, tok, punct, rec, w)
    rec.add("\n")  # end of title
    offset += 1
    if pnode is not None:
        rec.end(pnode)
    return offset


def pad(offset, token, punct, rec, w):
    """add padding whitespace if necessary:
    - after commas
    - after periods following on letters (not digits), and with a few exceptions
    - after empty punctuation"""
    punct_ends_word = is_end_of_word_punct(punct, token, w)
    word_ends_at_eol = punct == '' and token[-1] != '¬'  # token can be empty but never together with punct
    if word_ends_at_eol or punct == ',' or punct_ends_word:
        rec.add(" ")
        offset += 1
    return offset


def is_end_of_word_punct(punct, token, w):
    return punct == '.' \
           and WORD.match(token) and not DIGITS.match(token) \
           and not inword_period(token, w)


def inword_period(token, w):
    next_token = F.trans.v(L.n(w, otype='word')[0])
    return token == 'Comp' and next_token in COMP_ENDS \
           or token == 'S' and next_token in S_ENDS \
           or token in R_STARTS and next_token == 'r' \
           or token == 'R' and next_token == '1' \
           or token in RA_STARTS and next_token == 'ra' \
           or token == 'V' and next_token == 'O' \
           or token == 'O' and next_token in ['C', 'G', 'I'] \
           or token == 'I' and next_token == 'C' \
           or token == 'Eng' and next_token == '8e' \
           or token == 'Gomp' and next_token == 'es' \
           or token in N_STARTS and next_token == 'n'


def xpath_title(volume_id, letter_id):
    return "//volume[{}]/missive[{}]/title".format(volume_id, letter_id)


def xpath(volume_id, letter_id, section, section_id):
    return "//volume[{}]/missive[{}]/{}[{}]".format(volume_id, letter_id, section, section_id)


def tunit_info(volume_id, letter_id, section_type, unit_offset, offset, section_id=0):
    if not section_id:
        xp = "//volume[{}]/missive[{}]/{}".format(volume_id, letter_id, section_type)
    else:
        xp = xpath(volume_id, letter_id, section_type, section_id)
    return xp, unit_offset, offset - unit_offset, section_type


def record_typed_sequences(volume_id, letter_id, typed_seqs, letter):
    """records text and returns tunit info and recorder;
    remarks and footnotes are mixed and appear in order of appearance, i.e. footnotes are ordered by their mark
    :param letter: """
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
    return word_type == 'r' or word_type == 'n'


def starts_new_sequence(word_type, previous_type):
    return word_type != previous_type


def title(letter):
    """title words are the first part of the original text that do not belong to a paragraph"""
    title_seq = []
    for w in L.d(letter, otype='word'):
        para = L.u(w, otype='para')
        if F.isorig.v(w) and not para:
            title_seq.append(w)
        if para:
            break
    return title_seq


def trans(word_seq):
    return ''.join(["{}{}".format(F.trans.v(w), F.punc.v(w)) for w in word_seq])


def list_paragraphs(letter):
    return [L.d(p, otype='word') for p in L.d(letter, otype='para')]


def list_remarks(letter):
    return list_words(letter, lambda w: F.isremark.v(w))


def list_footnotes(letter):
    return list_words(letter, lambda w: F.isnote.v(w))


def is_notelike(w):
    return F.isnote.v(w) or F.isremark.v(w)


def is_inparagraph(w):
    return L.u(w, otype='para')


def text_sequences(letter):
    seqs = []
    seq = []
    types = []
    all_words = L.d(letter, otype='word')
    i = 0
    starts_new_text_unit = True
    while i < len(all_words):
        if F.isorig.v(all_words[i]):
            paragraph = L.u(all_words[i], otype='para')
            if paragraph:  # append all paragraph
                pwords = L.d(paragraph[0], otype='word')
                if seq:
                    seqs.append(seq)
                    types.append('header')
                seqs.append(pwords)
                types.append('paragraph')
                seq = []
                starts_new_text_unit = True
                i += len(pwords)
            else:
                if starts_new_text_unit and seq:  # flush previous seq
                    seqs.append(seq)
                    types.append('header')
                    seq = []
                seq.append(all_words[i])
                starts_new_text_unit = False
                i += 1
        else:
            starts_new_text_unit = True
            i += 1

    if seq:
        seqs.append(seq)
        types.append('header')
    return zip(seqs, types)


def mixed_note_sequences(letter):
    seqs = list_words(letter, is_notelike)
    types = []
    for seq in seqs:
        if F.isnote.v(seq[0]):
            types.append('footnote')
        else:
            types.append('remark')
    return zip(seqs, types)


def list_words(letter, to_list):
    seqs = []
    seq = []
    starts_new_text_unit = True
    for w in L.d(letter, otype='word'):
        if to_list(w):
            if starts_new_text_unit and seq:  # flush previous seq
                seqs.append(seq)
                seq = []
            seq.append(w)
            starts_new_text_unit = False
        else:
            starts_new_text_unit = True

    if seq:
        seqs.append(seq)
    return seqs


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
        )
    }
    return metaData
