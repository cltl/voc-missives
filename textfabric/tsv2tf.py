import sys
from utils.tfHandler import MissivesLoader


if __name__ == "__main__":
    tfdir = sys.argv[1]
    tsvdir = sys.argv[2]
    tfsavedir = sys.argv[3]
    ml = MissivesLoader()
    ml.record_entities_as_tf_features(tfdir, tsvdir, location=tfsavedir)
