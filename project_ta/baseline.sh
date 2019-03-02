#java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/twitter100/cite.txt" -paperfolder "./data/twitter100/tweet/" -aspectfile "./data/twitter100/aspect.txt" -samplerId "twitter_100" -znum 50 -burnin 100 -duplicate yes -model oaim

#java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/twitter500/cite.txt" -paperfolder "./data/twitter500/tweet/" -aspectfile "./data/twitter500/aspect.txt" -samplerId "twitter_500" -znum 50 -burnin 100 -duplicate yes -model oaim

#java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/twitter1000/cite.txt" -paperfolder "./data/twitter1000/tweet/" -aspectfile "./data/twitter1000/aspect.txt" -samplerId "twitter_1000" -znum 50 -burnin 100 -duplicate yes -model oaim

#java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/twitter1500/cite.txt" -paperfolder "./data/twitter1500/tweet/" -aspectfile "./data/twitter1500/aspect.txt" -samplerId "twitter_1500" -znum 50 -burnin 100 -duplicate yes -model oaim

#java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/twitter2000/cite.txt" -paperfolder "./data/twitter2000/tweet/" -aspectfile "./data/twitter2000/aspect.txt" -samplerId "twitter_2000" -znum 50 -burnin 100 -duplicate yes -model oaim

for l in 0.15 0.5 0.85
do
    java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/citeseerx_data/pubidcite.txt" -paperfolder "./data/citeseerx_data/paper_chu/" -aspectfile "./data/citeseerx_data/aspect.txt/" -samplerId "citeseerx" -znum 50 -burnin 100 -duplicate yes -model oaim -lambda $l
done

for l in 0.15 0.5 0.85
do
    java -Xmx10G  -cp infdetection.jar chu.BaseLineMethod -chainNum 2 -graphfile "./data/twitter500/cite.txt" -paperfolder "./data/twitter500/tweet/" -aspectfile "./data/twitter500/aspect.txt" -samplerId "twitter_500" -znum 50 -burnin 100 -duplicate yes -model oaim -lambda $l
done
