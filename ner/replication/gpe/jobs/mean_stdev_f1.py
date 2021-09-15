import statistics
import json
import sys
import os
import math

TEXTTYPES = ['text', 'notes', 'all']
DATASETS = ['train', 'dev', 'test']
MODELS = ['bertje', 'robbert', 'mbert', 'roberta']
SEEDS = ['seed1', 'seed10', 'seed100']
LABELS = ['GPE', 'LOC', 'LOCderiv', 'ORG', 'PER', 'RELderiv', 'SHP', 'overall']


def init_stats():
    stats = {}
    for model in MODELS:
        mdict = {}
        for label in LABELS:
            mdict[label] = []
        stats[model] = mdict
    return stats


def mean_stdev(f1scores, extra_small):
    if f1scores:
        mean = statistics.mean(f1scores)
        stdev = statistics.stdev(f1scores)
        if extra_small:
            return "{{\\small ${}$ {{\\scriptsize (${}$)}}}}".format(round(mean*100, 1), round(stdev*100, 1))
        else:
            return "${}$ {{\\small (${}$)}}".format(round(mean*100, 1), round(stdev*100, 1))
    return '-'


def compute_mean_and_stdev(results, header, extra_small=False):
    print(" & -GPE & +GPE\\ML")
    for label in header:
        scores = [mean_stdev(results[item][label], extra_small) for item in ['loc', 'gpe']]
        print("{} & {}\\NN".format(label, " & ".join(scores)))


def compute_mean_and_stdev_models(results, models, labels, extra_small=False):
    print(" & -GPE & +GPE\\ML")
    for label in labels:
        scores1 = [mean_stdev(results[item][models[0]][label], extra_small) for item in ['loc', 'gpe']]
        scores2 = [mean_stdev(results[item][models[1]][label], extra_small) for item in ['loc', 'gpe']]
        print("{{\\small {}}} & {} & {}\\NN".format(label, " & ".join(scores1), " & ".join(scores2)))


def detailed_stats(results_dir_gpe, results_dir_loc):
    results = {}
    results['loc'] = {'bertje': {}, 'mbert': {}}
    results['gpe'] = {'bertje': {}, 'mbert': {}}
    for exp in ['loc', 'gpe']:
        for model in ['bertje', 'mbert']:
            for label in LABELS:
                results[exp][model][label] = []
    for model in ['mbert', 'bertje']:
        for seed in SEEDS:
            with open(os.path.join(results_dir_gpe, "{}_all_{}_all_predict_results.json".format(seed, model))) as f:
                rdict = json.load(f)
                for label in LABELS:
                    results['gpe'][model][label].append(rdict["predict_{}_f1".format(label)])
            with open(os.path.join(results_dir_loc, "{}_all_{}_all_predict_results.json".format(seed, model))) as f:
                rdict = json.load(f)
                for label in LABELS:
                    if label == 'GPE':
                        continue
                    results['loc'][model][label].append(rdict["predict_{}_f1".format(label)])
    compute_mean_and_stdev_models(results, ['mbert', 'bertje'], LABELS, True)


def stats(results_dir_gpe, results_dir_loc):
    results = {}
    results['loc'] = {}
    results['gpe'] = {}
    for model in MODELS:
        results['loc'][model] = []
        results['gpe'][model] = []

        for seed in SEEDS:
            with open(os.path.join(results_dir_gpe, "{}_all_{}_all_predict_results.json".format(seed, model))) as f:
                rdict = json.load(f)
                results['gpe'][model].append(rdict["predict_overall_f1"])
            with open(os.path.join(results_dir_loc, "{}_all_{}_all_predict_results.json".format(seed, model))) as f:
                rdict = json.load(f)
                results['loc'][model].append(rdict["predict_overall_f1"])
    compute_mean_and_stdev(results, MODELS)


if __name__ == "__main__":
    detailed_stats(sys.argv[1], sys.argv[2])
    stats(sys.argv[1], sys.argv[2])

