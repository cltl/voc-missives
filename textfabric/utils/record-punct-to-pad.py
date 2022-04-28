import os
import sys

from utils.tfHandler import MissivesLoader


def export_punct_topad(outdir):
    """Export tokens with missing whitespace after punctuation from TF to text
    :param outdir: the output directory for TF output
    :param ml: a MissivesLoader, possibly preloaded
    """
    ml = MissivesLoader()
    os.makedirs(outdir, exist_ok=True)
    outfile = os.path.join(outdir, 'punct_topad')
    rec = ml.record_punct_topad()
    rec.write(outfile)


if __name__ == "__main__":
    outdir = sys.argv[1]
    export_punct_topad(outdir)
