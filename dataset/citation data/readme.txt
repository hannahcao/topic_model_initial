Influence_Annotation.txt: the manually labeled influence strength. 

Annotation spec.doc: the specification for type and influence annotation

data.txt the details for the citation data

input.txt the input data after cleaning

ACCT.exe the executive program

ACCT -est -dfile input.txt -ntopics 10 -niters 4000 -alpha 0.01 -savestep 100 -startliter 3000 -twords 10

ntopics: the number of topics
niters: the number of iteration
alpha: the Dirichlet parameter
savestep: the interval to save the intermediate result in the sub-directory "tmp"
startliter: the start iteration to save the intermediate result
twords: the number of keywords shown in each topic