from utils.tfHandler import MissivesLoader, make_features, get_file_features

import os


def test_create_features():
    workdir = 'tests/data/tsv2tf'
    file_feats = get_file_features(os.path.join(workdir, 'missive_1_49_notes'), os.path.join(workdir, 'missive_1_49_notes.tsv'))
    assert file_feats['entityId']
    features = make_features(workdir, workdir)
    assert features['entityId']
    assert len(features['entityId'].items()) > len(file_feats['entityId'].items())


def test_save_features():
    ml = MissivesLoader()
    workdir = 'tests/data/tsv2tf'
    features = make_features(workdir, workdir)
    ml.save_to_tf(features)
