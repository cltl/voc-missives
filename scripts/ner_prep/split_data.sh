#!/bin/bash
#
# derives test/dev/train files for notes and notes+text
# from the selected missives
# --------------------------------------------
data=$1

outNotes=$data/systemIn/notes
outAll=$data/systemIn/all
outText=$data/systemIn/text

[[ ! -d $outNotes ]] && mkdir $outNotes
[[ ! -d $outAll ]] && mkdir $outAll
[[ ! -d $outText ]] && mkdir $outText

cp $data/datasplit/notes/dev/* $outNotes/dev.conll
cp $data/datasplit/notes/test/* $outNotes/test.conll
cat $data/datasplit/notes/train/* > $outNotes/train.conll

cp $data/datasplit/text/dev/* $outText/dev.conll
cp $data/datasplit/text/test/* $outText/test.conll
cat $data/datasplit/text/train/* > $outText/train.conll

cat $data/datasplit/text/dev/* $outNotes/dev.conll > $outAll/dev.conll
cat $data/datasplit/text/test/* $outNotes/test.conll  > $outAll/test.conll
cat $data/datasplit/text/train/* $outNotes/train.conll > $outAll/train.conll
