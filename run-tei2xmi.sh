#!/bin/bash

indir=$1
outdir=$2

java -jar target/voc-missives-1.0-SNAPSHOT-jar-with-dependencies.jar ${indir} ${outdir} 
