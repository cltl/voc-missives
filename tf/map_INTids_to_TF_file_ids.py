import glob
import re
import json
import os
import argparse
import shutil

from utils.tfHandler import MissivesLoader
# ex. vol1_p0106_INT_08c82040-752f-3fc2-ad50-0d3e7b37a945_text.naf
VPINT = re.compile(r"vol(\d+)_p(\d+)_(INT_.*)_(\w+)\.naf")
INT1 = re.compile(r"(.*)(INT_.*)_notes\.(.*)")
INT2 = re.compile(r"(.*)(INT_.*)\.(.*)")


def get_int_annotated_files(int_ids_dir):
    return [file for file in glob.iglob("{}/**".format(int_ids_dir), recursive=True) if os.path.isfile(file)]


def pattern_matches(files, matcher):
    return [matcher(os.path.basename(file)) for file in files]


def match_vpint(filename):
    m = VPINT.match(filename)
    return int(m[1]), int(m[2]), m[3], m[4]


def match_int(filename):
    m = INT1.match(filename)
    if m is not None:
        return m[2], 'notes', m[3]
    else:
        m = INT2.match(filename)
        return m[2], 'text', m[3]


def get_tf_ids(matches):
    ml = MissivesLoader()
    volumes_pages_and_types = [(v, l, tt) for v, l, _, tt in matches]
    return ml.tf_ids(volumes_pages_and_types)


def json_obj(input_files, matches, tf_ids):
    dicts = []
    for tf_id, f, (_, _, int_id, _) in zip(tf_ids, input_files, matches):
        dicts.append({'int_id': int_id, 'tf_id': tf_id, 'int_id_file': f})
    return dicts


def get_target_filenames(matches, dicts):
    def trgname(i, t, ext):
        m = [e for e in dicts if e['int_id'] == i and t in e['tf_id']]
        return "{}.{}".format(m[0]['tf_id'], ext)
    return [trgname(intid, ftype, ext) for intid, ftype, ext in matches]


def copy_files(input_files, outdir, target_filenames):
    for file, trg in zip(input_files, target_filenames):
        shutil.copy(file, os.path.join(outdir, trg))


def rename_intid_files_to_tfids(int_dir, json_file, outdir):
    os.makedirs(outdir, exist_ok=True)
    input_files = get_int_annotated_files(int_dir)
    matches = pattern_matches(input_files, match_int)
    dicts = load(json_file)
    copy_files(input_files, outdir, get_target_filenames(matches, dicts))


def load(json_file):
    with open(json_file) as f:
        dicts = json.load(f)
    return dicts


def create_json(int_ids_dir, json_outfile):
    input_files = get_int_annotated_files(int_ids_dir)
    # the INT id filenames should match the VPINT pattern
    matches = pattern_matches(input_files, match_vpint)
    tf_ids = get_tf_ids(matches)
    with open(json_outfile, 'w') as f:
        json.dump(json_obj(input_files, matches, tf_ids), f, indent=2)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='mapping INT file ids to TF ids')
    parser.add_argument('-j', '--json_file', type=str, help='json mapping file')
    parser.add_argument('-c', '--create_json_file', action='store_true', help='create json mapping file')
    parser.add_argument('-i', '--int_dir', type=str, help='input dir with INT id files')
    parser.add_argument('-o', '--out_dir', type=str, help='output dir with remapped files')
    args = parser.parse_args()

    if args.create_json_file:
        create_json(args.int_dir, args.json_file)
    else:
        rename_intid_files_to_tfids(args.int_dir, args.json_file, args.out_dir)
