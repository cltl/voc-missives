#!/bin/bash

# counts words and sentences in conll file
# -------------------------------------------

lines=$(cat $1 | wc -l)
sentences=$(grep "^$" $1 | wc -l)
tokens=$((lines - sentences))
entities=$(grep "[^O]$" $1 | wc -l)

echo "$sentences sentences, $tokens tokens, $entities entities"