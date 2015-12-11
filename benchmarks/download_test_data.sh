#!/usr/bin/env bash

# Canterbury Corpus large dataset
# http://corpus.canterbury.ac.nz

BASE_PATH="`dirname \"$0\"`"
cd $BASE_PATH

if [ \! -d "data" ]; then
  mkdir -p "data"
fi

if [ \! -e "data/large.tar.gz" ]; then
  curl -Lo "data/large.tar.gz" http://corpus.canterbury.ac.nz/resources/large.tar.gz
fi

if [ ! -e "data/E.coli" ]; then
  tar xzf data/large.tar.gz -C data/ E.coli
fi
