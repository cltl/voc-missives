import sys
import os
import json

TEXTTYPES = ['text', 'notes', 'all']
MODELS = ['mbert', 'bertje', 'xlmr', 'robbert']


def json2conll(injson, outconll):
    pass


def glue_predictions(indir, outdir):
    for test in TEXTTYPES:
        for model in MODELS:
            ref_f = os.path.join(indir, "{}_{}_test.json".format(test, model))
            with open(ref_f) as f:
                json_seqs = json.load(f)
            for train in TEXTTYPES:
                sys_f = os.path.join(indir, "{}_{}_{}_predictions.txt".format(train, model, test))
                with open(sys_f) as f:
                    preds = f.readlines()
                for ref, pred in zip(json_seqs, preds):
                    json_seqs['sys'] = pred




if __name__ == "__main__":
    glue_predictions(sys.argv[1], sys.argv[2])
