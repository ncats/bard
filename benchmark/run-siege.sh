SIEGE=/usr/local/bin/siege
BENCHDIR=`pwd`
n_concurrent=25
n_reps=10
$SIEGE -v -c $n_concurrent -i -b -r $n_reps -f urls.txt -R $BENCHDIR/siegerc  -l 

