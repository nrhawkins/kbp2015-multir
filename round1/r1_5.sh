#!/bin/bash

sbt "run-main
edu.washington.cs.knowitall.kbp2014.multir.slotfiller.RunKBP2015MultirExtractorCorpusSerialized
/homes/gws/nhawkins/KBP2015-Slotfilling-Multir/queries2015_r1.xml 0 cs
/homes/gws/nhawkins/KBP2015-Slotfilling-Multir/relDocs_queries2015_r1 
/homes/gws/nhawkins/KBP2015-Slotfilling-Multir/round1/out/out_multir_r1_5 round1 102 329 
/projects/WebWare6/KBP_2015/corpus/serialized/"

exit 0
