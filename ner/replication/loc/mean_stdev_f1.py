import statistics
import json
import sys
import os


TEXTTYPES = ['text', 'notes', 'all']
DATASETS = ['train', 'dev', 'test']
MODELS = ['bertje', 'robbert', 'mbert', 'roberta']
SEEDS = ['seed1', 'seed10', 'seed100']
LABELS = ['LOC', 'LOCderiv', 'ORG', 'PER', 'RELderiv', 'SHP', 'overall']


def init_stats():
    stats = {}
    for model in MODELS:
        mdict = {}
        for label in LABELS:
            mdict[label] = []
        stats[model] = mdict
    return stats


def mean_stdev(f1scores):
    mean = statistics.mean(f1scores)
    stdev = statistics.stdev(f1scores)
    return "${}$ {{\\small (${}$)}}".format(round(mean*100, 1), round(stdev*100, 1))

def mean_stdev_small(f1scores):
    mean = statistics.mean(f1scores)
    stdev = statistics.stdev(f1scores)
    return "{{\\small ${}$ {{\\scriptsize (${}$)}}}}".format(round(mean*100, 1), round(stdev*100, 1))


def mname(model):
    if model == 'bertje':
        return 'BERTje'
    elif model == 'robbert':
        return 'RobBERT'
    elif model == 'mbert':
        return 'mBERT'
    else:
        return 'XLM-R$_{base}$'


def compute_mean_and_stdev(results, header):
    print(" & {} \\ML".format(" & ".join(header)))
    for model in MODELS:
        scores = [mean_stdev(results[model][item]) for item in header]
        print("{} & {} \\NN".format(mname(model), " & ".join(scores)))


def compute_mean_and_stdev_ood(results, header):
    print(" & {} \\ML".format(" & ".join(header)))
    for model in MODELS:
        print("\\multirow{{3}}{{*}}{{{}}} & text & \\textcolor{{gray}}{{{}}} & {} \\NN".format(mname(model), mean_stdev(results[model]['text/text']), mean_stdev(results[model]['text/notes'])))
        print(" & notes & {} & \\textcolor{{gray}}{{{}}} \\NN".format(mean_stdev(results[model]['notes/text']), mean_stdev(results[model]['notes/notes'])))
        print(" & all & {} & {} \\NN".format(mean_stdev(results[model]['all/text']), mean_stdev(results[model]['all/notes'])))


def stats(results_dir):
    for fdata in TEXTTYPES:
        for tdata in TEXTTYPES:
            print("Fine-tuning: {}\tTesting: {}".format(fdata, tdata))
            results = init_stats()
            for model in MODELS:
                for seed in SEEDS:
                    with open(os.path.join(results_dir, "{}_{}_{}_{}_predict_results.json".format(seed, fdata, model, tdata))) as f:
                        rdict = json.load(f)
                        for label in LABELS:
                            results[model][label].append(rdict["predict_{}_f1".format(label)])
            compute_mean_and_stdev(results, LABELS)
            print()


def summary_stats_oodomain(results_dir):
    results = {}
    columns = []
    for fdata in TEXTTYPES:
        for tdata in TEXTTYPES:
            if tdata == 'all':
                continue
            columns.append("{}/{}".format(fdata, tdata))
    for model in MODELS:
        results[model] = {}
        for col in columns:
            results[model][col] = []

    for fdata in TEXTTYPES:
        for tdata in TEXTTYPES:
            if tdata == 'all':
                continue
            key = "{}/{}".format(fdata, tdata)
            for model in MODELS:
                for seed in SEEDS:
                    with open(os.path.join(results_dir, "{}_{}_{}_{}_predict_results.json".format(seed, fdata, model, tdata))) as f:
                        rdict = json.load(f)
                        results[model][key].append(rdict["predict_overall_f1"])
    compute_mean_and_stdev_ood(results, columns)
    print()


def summary_stats_indomain(results_dir):
    results = {}
    columns = []
    for fdata in TEXTTYPES:
        for tdata in TEXTTYPES:
            if tdata == 'all' and fdata != 'all' or tdata != fdata:
                continue
            columns.append("{}/{}".format(fdata, tdata))
    for model in MODELS:
        results[model] = {}
        for col in columns:
            results[model][col] = []

    for fdata in TEXTTYPES:
        for tdata in TEXTTYPES:
            if tdata == 'all' and fdata != 'all' or tdata != fdata:
                continue
            key = "{}/{}".format(fdata, tdata)
            for model in MODELS:
                for seed in SEEDS:
                    with open(os.path.join(results_dir, "{}_{}_{}_{}_predict_results.json".format(seed, fdata, model, tdata))) as f:
                        rdict = json.load(f)
                        results[model][key].append(rdict["predict_overall_f1"])
    compute_mean_and_stdev(results, columns)
    print()


def detailed_stats(results_dir):
    results = {}
    columns = ['text/text', 'all/text', 'notes/notes', 'all/notes']
    for data in columns:
        fdata, tdata = data.split('/')[0], data.split('/')[1]
        results[data] = {}
        for label in LABELS:
            results[data][label] = []
        for seed in SEEDS:
            with open(os.path.join(results_dir,
                                   "{}_{}_{}_{}_predict_results.json".format(seed, fdata, 'roberta', tdata))) as f:
                rdict = json.load(f)
                for label in LABELS:
                    results[data][label].append(rdict["predict_{}_f1".format(label)])
    print(" & {} \\ML".format(" & ".join(['text/text', 'all/text', 'notes/notes', 'all/notes'])))
    for label in LABELS:
        scores = [results[data][label] for data in columns]
        print("{{\\small {}}} & {} \\NN".format(label, " & ".join([mean_stdev_small(score) for score in scores])))


if __name__ == "__main__":
    stats(sys.argv[1])
    print('--------------------\n')
    summary_stats_indomain(sys.argv[1])
    summary_stats_oodomain(sys.argv[1])
    detailed_stats(sys.argv[1])