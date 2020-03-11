#!/bin/bash
dir=$1 # output files dir

for b in `seq 0 299`; do
	c=8
	while [ $c -le 256 ]
	do
		d=1
		while [[ $d -le 8 ]]
		do
			./generator.exe $c $d > $dir/$d"_"$c"_"$b.in
			((d=$d*2))
		done
		sleep 0.2
		((c=$c*2))
	done
	#sleep 0.2
done
