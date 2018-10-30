# Problem

Given input files wieh data for visa applications, output 2 files. One need to output top 10 occupations certified applications' names, certified applications' numbers with their percentages, the other is to output top 10 states of certified applications' names, certified applications' numbers with their percentages.

# Approach

In one file, read line by line. Use case_status as the delimitation of applications.
At the same time, store every certified application data in occupation map and state map.
Use 10-size min heap to store occupation and state, then add it reversely and its matching application numbers with percentages.

# Run Instruction
Therea are three arguments, the first is the path of input file, the second is the path of output 10 top occupation file, the third is the path of 10 top output states file.
