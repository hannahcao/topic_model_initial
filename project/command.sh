
# Print the help information
java -cp infdetection.jar cao.MainInfDetection -help

# Use all default values to test
#java -cp infdetection.jar cao.MainInfDetection -samplerId "testdata" > output/test.txt

#
#java -cp infdetection.jar cao.MainInfDetection -samplerId "testdata" -numIter 10 > output/test_numIter10.txt

#java -cp infdetection.jar cao.MainInfDetection -graphfile "data/pubidcite.txt" -paperfolder data/paper/ -samplerId "dietzdata.20130218"

java -cp infdetection.jar cao.MainInfDetection -graphfile "data/pubidcite.txt" -paperfolder data/paper/ -samplerId "dietzdata.20130218" -znum 30
