
# Print the help information
#java -cp infdetection.jar cao.MainInfDetection -help

# Use all default values to test
#java -cp infdetection.jar cao.MainInfDetection -samplerId "testdata" > output/test.txt

# after using aspect count, we can't use this default data to do test.
#java -cp infdetection.jar cao.MainInfDetection -samplerId "testdata" -numIter 10 > output/test_numIter10.txt

#dietz data
#java -cp infdetection.jar cao.MainInfDetection -graphfile "data/pubidcite.txt" -paperfolder data/paper/ -samplerId "dietzdata.20120911"



#for z in 10 20 30 40 50 60 70 80 90 100
#do
#    java -cp infdetection.jar cao.MainInfDetection -graphfile "./data/twitter/cite.txt" -paperfolder "./data/twitter/tweet/" -aspectfile "./data/twitter/aspect.txt" -samplerId "twitter_ta z "$z -znum $z -burnin 100 -duplicate yes
#done

#java -cp infdetection.jar cao.MainInfDetection -chainNum 2 -graphfile "./data/citeseerx_data/pubidcite.txt" -paperfolder "./data/citeseerx_data/paper_chu/" -aspectfile "./data/citeseerx_data/aspect.txt" -samplerId "citeseerx_data_ta" -znum 10 -burnin 100 -duplicate yes -model oaim

#-Xmx4G
java -Xmx10G  -cp infdetection.jar cao.MainInfDetection -chainNum 2 -graphfile "./data/twitter500/cite.txt" -paperfolder "./data/twitter500/tweet/" -aspectfile "./data/twitter500/aspect.txt" -samplerId "twitter_500_oaim z "50" a "50 -znum 50 -anum 50 -burnin 100 -duplicate yes -model laim
