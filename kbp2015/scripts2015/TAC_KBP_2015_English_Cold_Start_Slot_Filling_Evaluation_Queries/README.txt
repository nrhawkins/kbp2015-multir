            TAC KBP 2015 English Cold Start Slot Filling Evaluation Queries 

                            August 12, 2015

					  
1. Overview

This package contains 9339 queries created for use in the TAC 2015
KBP Cold Start Slot Filling evaluation.

For more information, please refer to the Cold Start section of
NIST's TAC website at
http://www.nist.gov/tac/2015/KBP/ColdStart/index.html


2. Contents

./README.txt

  This file.

./data/tac_kbp_2015_english_cold_start_slot_filling_evaluation_queries.xml

  This file contains 9339 queries. Each query consists of the following
  6 or 7 elements:

      1. The namestring of the entity 
      2. The source document (from LDC2015E77) 
      3. The start offset 
      4. The end offset  
      5. The entity's type (PER, ORG, or GPE)
      6. The first slot
      7. The second slot (optional, as some queries only have one slot)
 
  Note that each Cold Start Slot Filling query has an identifier,
  formatted as the letters "CSSF15_ENG_" plus ten alphanumeric
  characters (e.g., "fff88c70dc").
  
  
./dtd/cold_start_slot_filling_queries_2015.dtd

  The DTD for
  tac_kbp_2015_english_cold_start_slot_filling_evaluation_queries.xml


3. Text Normalization

Text normalization consisting of substitution of single newline
(0x0A) and tab (0x09) characters and multiple space (0x20) characters 
for a single space (0x20) character was performed on the document 
text input to the response field.

