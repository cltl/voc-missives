import sys
import json
import os

TEXTTYPES = ['text', 'notes', 'all']
DATASETS = ['train', 'dev', 'test']
MODELS = ['mbert', 'bertje', 'roberta', 'robbert']


def read_predictions(predictions):
    with open(predictions) as f:
        scores = json.load(f)
    return scores


def read_prediction_results(indir, out_csv):
    with open(out_csv, 'w') as f:
        for train in TEXTTYPES:
            for model in MODELS:
                for test in TEXTTYPES:
                    scores = read_predictions(os.path.join(indir, "{}_{}_{}_predict_results.json".format(train, model, test)))
                    for k, v in scores.items():
                        f.write("{},{},{},{},{}\n".format(model, train, test, k, v))


if __name__ == "__main__":
    read_prediction_results(sys.argv[1], sys.argv[2])
