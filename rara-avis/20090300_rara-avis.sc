// rara avis / ALKU bird sound installation

// projects
// notes
// sound
// old bird / world code, obiwannabe
// and finally: arbirds
// adequate locomotion strategies: 

(
s.boot;
Server.default = s;
//SwingOSC.java;
//SwingOSC.java = "/usr/lib/jvm/java-1.5.0-sun-1.5.0.16/bin/java";
// ~g1 = SwingOSC.default;
// ~g1.boot;
)

(
~asbus = Bus.audio(s, 3);
~asbus.index;
~noisebus = Bus.audio(s, 1);
)

~g1.quit;
~g1.serverRunning_(false)
~g1.dump

//{SinOsc.ar()}.scope

s.scope();

// ambisonic output layer
(
SynthDef(\asdec, {|inchan=0|
	#w, x, y = In.ar(inchan, 3);
	Out.ar(0, DecodeB2.ar(2, w, x, y));
}).store;
)

// birds: synth(s)
// fill in different bird synth methods
// will start off with fof / formlet synths
( // test synth
SynthDef(\pingbird1, {
	|out=0, amp = 1.0, dur = 0.1, freq = 1000.0, form = 2300.0, forw = 200.0|
	var gate, az;
	gate = Trig.kr(1.0, dur);
	az = 0;
	Out.ar(out, PanB2.ar(Formant.ar(freq, form, forw) * Linen.kr(gate,
		0.001, 1.0, 0.05, doneAction: 2) * amp), az, 1.0);
}).store;
)

o = Synth(\asdec, [\inchan, ~asbus.index]);
x = Synth(\pingbird1, [\out, ~asbus.index, \freq, 300, \amp, 0.01, \dur, 0.01]);

// check
Env({ rrand(0.1, 0.2) }!20, ({rrand(1, 20)}!(20-1)).normalizeSum, {rrand(1, 6).neg}!20).plot

//Env([0, 1], [1], -10).plot

//({rrand(0, 6).neg}!20).plot


( // every bird
SynthDef(\fofsynth1, {|out=0, amp= 0.707, dur=5, freqs=#[440, 880], ffreqs = #[733, 866, 333, 444]|
	var sig, penv, penvg, aenv, aenvg, fenv, fenvg;
	var numform = 4;
	var envelem = 20;
	// trigger main amplitude envelope
	// envelope coupling (wishart)
	// envelopes: 30-40 elements, repetition
	// add noise for plosives
	penv = Env(
		{ Rand(0.01, 0.99) }!envelem,
		({ Rand(1, 20) }!(envelem-1)).normalizeSum,
		{ -1 }!envelem
	);
	penvg = {|i| EnvGen.ar(penv, Impulse.kr(0), freqs[i], 0.0, dur) }
	! 2; // 2 syrinxes
	sig = Blip.ar(penvg /*penvg*/, 70, 0.4);
	//sig = Impulse.ar(freqs, 0, 0.4);
	// envelopes + jitter, also try use polynomials for the envelope
	fenv = { Env({Rand(0.01, 0.99)}!envelem, ({1}!(envelem-1)).normalizeSum, {-1}!envelem) } ! numform;
	fenvg = {|i| EnvGen.kr(fenv[i], Impulse.kr(0), ffreqs[i], 0.0, dur) } ! numform;
	sig = Formlet.ar(sig, fenvg /*Line.ar(1, ffreqs, dur)*/, 0.01, 0.1);
	aenv = Env([0.0, 1.0, 1.0, 0.0], [1, 18, 1].normalizeSum, [1, 1, 1, 1]);
	//aenv = Env.asr(0.05, 1.0, 0.3, -1);
	//aenv = Env.perc(0.1, 0.9, 1.0, -1);
	aenvg = EnvGen.ar(aenv, Impulse.kr(0), amp, 0.0, dur, doneAction: 2);
	Out.ar(out, Mix(sig*aenvg).dup);
}).store;
)

//{|i| Env.perc} ! 4

x = Synth(\fofsynth1, [\out, 0, \amp, 0.1, \dur, 2]);
(
x = Synth(\fofsynth1, [
	\out, 0, \amp, 0.02, \dur, 0.3,
	\freqs, [rrand(200, 10000), rrand(200, 1000)],
	\ffreqs, {rrand(200, 10000)}!4
]);
)
(
x = Synth(\fofsynth1, [
	\out, 0, \amp, 0.2, \dur, 1.2,
	\freqs, [rrand(200, 1000), rrand(200, 1000)],
	\ffreqs, [rrand(200, 10000), rrand(200, 1000)]
]);
)

s.scope;
Env.methods.dump

#{1}!10

( // every bird
// version 2 defunct with the arrays
SynthDef(\fofsynth2, {|out=0, amp= 0.707, dur=5,
	freqs=#[ // frequency values
		[0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1],
		[1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0]
	],
	freqsl=#[ // frequency durations
		[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
		[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
	],
	freqsc=#[ // frequency curvatures
		[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
		[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]
	],
	freqsb=#[440, 770], // frequency bases
	ffreqs = #[ // fof frequency values
		[0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1],
		[1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0],
		[0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2],
		[2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0]
	],
	ffreqsl=#[ // frequency durations
		[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
		[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
		[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
		[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
	],
	ffreqsc=#[ // frequency curvatures
		[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
		[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
		[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
		[-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]
	],
	ffreqsb=#[440, 770, 833, 330] /* frequency bases */ |
	var sig, penv, penvg, aenv, aenvg, fenv, fenvg, trig;
	var numform = 4;
	var envelem = 20;
	// trigger main amplitude envelope
	trig = Impulse.ar(0.2);
	// envelope coupling (wishart)
	// envelopes: 30-40 elements, repetition
	// add noise for plosives
	penv = {|i| Env(
		freqs[i],
		freqsl[i].normalizeSum,
		freqsc[i]
	)} ! 2;
	penvg = {|i| EnvGen.ar(penv[i], trig, freqsb[i], 0.0, dur) }
	! 2; // 2 syrinxes
	sig = Blip.ar(penvg /*penvg*/, 40, 0.4);
	//sig = Impulse.ar(freqs, 0, 0.4);
	// envelopes + jitter, also polynomials
	fenv = {|i| Env(ffreqs[i], ffreqsl[i].normalizeSum, ffreqsc[i]) } ! numform;
	fenvg = {|i| EnvGen.kr(fenv[i], trig, ffreqsb[i], 0.0, dur) } ! numform;
	//sig = Formlet.ar(sig, fenvg /*Line.ar(1, ffreqs, dur)*/, 0.01, 0.1);
	aenv = Env([0.0, 1.0, 1.0, 0.0], [1, 18, 1].normalizeSum, [1, 1, 1, 1]);
	//aenv = Env.asr(0.05, 1.0, 0.3, -1);
	//aenv = Env.perc(0.1, 0.9, 1.0, -1);
	aenvg = EnvGen.ar(aenv, trig, amp, 0.0, dur, doneAction: 0);
	//Out.ar(out, Mix(sig*aenvg).dup);
	Out.ar(out, sig*aenvg);
}).store;

////////////////////
// listener synth (fitness function)
SynthDef(\listener2, {|in=0, dur=1.0, bufn=0, amp=1.0|
	var chain, centroid, flatness, crest, flux, spread, trig;
	var inp = In.ar(in, 1);
	// SpecCentroid, FFTComplexDev, FFTCrest, FFTFlatness, FFTFlux,
	// FFTPercentile, FFTPower, Amplitude, Pitch, 
	/* Criteria we would like to have:
		- some LF component: tweedledeedledeedledeedledeet
        - spectral compactness / bandwidth
		- ...
	*/
	chain = FFT(bufn, inp);
	centroid = Integrator.kr(SpecCentroid.kr(chain));
	crest = Integrator.kr(SpecFlatness.kr(chain));
	spread = Integrator.kr(FFTSpread.kr(chain));
	flux = Integrator.kr(FFTFlux.kr(chain, 1.0));
	trig = Line.ar(-0.1, 0.001, dur, doneAction: 2);
	SendTrig.ar(trig, 100, centroid);
	SendTrig.ar(trig, 101, crest);
	SendTrig.ar(trig, 102, spread);
	SendTrig.ar(trig, 103, flux);
	SendTrig.ar(trig, 200, dur*SampleRate.ir/64);
	Out.ar(0, inp.dup * amp);
	//[centroid, spread].poll(10);
}).store;

// version 3: env arrays as controls
SynthDef(\fofsynth3, {|out=0, amp= 0.707, dur=1.0, plvl=1000.0,
	pbias=1000.0, flvl=4000.0, fbias=0.0, pnoiselvl = 0.0,
	fnoiselvl = 0.0|
	var sig, penv, penvg, fenv, fenvg, aenv, aenvg;
	var numform = 4;
	var envelems = 20;
	var trig = Impulse.kr(0);
	penv = {|i|
		Env(
			Control.names(["penv" ++ (i+1).asString ++ "d"]).ir(Array.series(envelems,0,0.1)),
			Control.names(["penv" ++ (i+1).asString ++ "t"]).ir(Array.series(envelems-1,1,0).normalizeSum),
			Control.names(["penv" ++ (i+1).asString ++ "c"]).ir(Array.series(envelems,-1,0))
		)} ! 2;
	penvg = {|i| EnvGen.kr(penv[i], trig, plvl, pbias, dur)}!2;
	sig = Blip.ar(penvg + LFNoise1.ar(10, pnoiselvl), 40, 0.4);
	fenv = {|i|
 		Env(
			Control.names(["fenv" ++ (i+1).asString ++ "d"]).ir(Array.series(envelems,0,0.1)),
			Control.names(["fenv" ++ (i+1).asString ++ "t"]).ir(Array.series(envelems-1,1,0).normalizeSum),
			Control.names(["fenv" ++ (i+1).asString ++ "c"]).ir(Array.series(envelems,-1,0))
		)} ! numform;
	fenvg = {|i| EnvGen.kr(fenv[i], trig, flvl, fbias, dur) } ! numform;
	sig = Formlet.ar(sig, fenvg + LFNoise1.ar(10, fnoiselvl) /*Line.ar(1, ffreqs, dur)*/, 0.01, 0.05);
	aenv = {|i|
		Env(
			Control.names(["aenv" ++ (i+1).asString ++ "d"]).ir(Array.series(envelems,0,0.1)),
			Control.names(["aenv" ++ (i+1).asString ++ "t"]).ir(Array.series(envelems-1,1,0).normalizeSum),
			Control.names(["aenv" ++ (i+1).asString ++ "c"]).ir(Array.series(envelems,-1,0))
		)} ! 1;
// 		[0.0, 1.0, 1.0, 0.0],
// 		[1, 18, 1].normalizeSum,
// 		[1, 1, 1, 1]);
	//aenv = Env.asr(0.05, 1.0, 0.3, -1);
	//aenv = Env.perc(0.1, 0.9, 1.0, -1);
	aenvg = EnvGen.ar(aenv[0], trig, amp, 0.0, dur, doneAction: 2);
	Out.ar(out, Mix(sig*aenvg).dup);
	//Out.ar(out, sig*aenvg);
}).store;

// fofsynth4

)

/* 20081110
	x evolution
	x read envs from file, bird brain als controller
	- neural: alles als signal, LF und tweets
	- modelling vs. selbsterzeugte strukur
    x amplitude envelope aussi: for figures, repetition
*/

/* 20081119
	- three possibilities for control
	 - Task based: each bird is a Task and Triggers the voice synth
	 - 2 Synths: brain, voice: brain synth triggers voice synth
	 x ! one synth: brain and voice together. init synth once, triggering internal
*/

x[0]
x[1].size

a = [(1..10), (1..10)]
a[0]

//{|i| Env.perc} ! 4

/* XXX
*/

s.boot;


x = Synth(\fofsynth2, [\out, 0, \amp, 0.1, \dur, 3]);
x.trace;

x = Synth(\fofsynth3, [\out, 0, \amp, 0.1, \dur, 3]);
x.trace;

(
var dur = 0.8;
s.sendBundle(nil,
	["/s_new", "fofsynth3", 2000, 1, 0, \amp, 0.1, \dur, dur, \out, ~bbus.index], // start note
	//["/n_setn", 2000, "penvd", 10] ++ Array.series(10, 1.0, -0.1) // set odd harmonics
	["/n_setn", 2000, "penv1d", 20] ++ ({rrand(0.01, 1.0)}!20), // set odd harmonics
	["/n_setn", 2000, "penv2d", 20] ++ ({rrand(0.01, 1.0)}!20),
	["/n_setn", 2000, "fenv1d", 20] ++ ({rrand(0.01, 1.0)}!20),
	["/n_setn", 2000, "fenv2d", 20] ++ ({rrand(0.01, 1.0)}!20),
	["/n_setn", 2000, "fenv3d", 20] ++ ({rrand(0.01, 1.0)}!20),
	["/n_setn", 2000, "fenv4d", 20] ++ ({rrand(0.01, 1.0)}!20),
	["/n_setn", 2000, "aenv1d", 20] ++ ({rrand(0.01, 1.0)}!20),
	["/s_new", "listener2", 2001, 1, 0, \amp, 1.0, \bufn, b.bufnum,
		\in, ~bbus.index]
	//	["/n_setn", 2000, "ring", 4] ++ Array.fill(4,0.1), // set shorter ring time
	//	["/n_setn", 2000, "amp", 4] ++ Array.fill(4,0.2) // set louder amps
);
s.sendBundle(dur,
 	["/n_free", 2001]);
// s.sendBundle(0.2,
// 	["/n_trace", 2000]);
)

////////////////////////////////////////
(
~ab_initbird = {|m=5|
	/* generate some random bird datafiles
		- generate tables and save them to file */
	//m = 5;
	f = File("birds/avis-data-bird-0-" ++ m.asString ++ ".dat", "w");
	r = [[0.1, 2.0], [100.0, 5000.0], [0.0, 500.0], [100.0, 12000.0],
		[0.0, 1000.0]]; // ranges for duration, env levels and offsets
	f.write(0.asString ++ " "); // init fitness to 0
	5.do {|i|
		l = rrand(r[i][0], r[i][1]); // duration
		f.write(l.asString ++ if(i<4, " ", Char.nl));
	};
	7.do {|i|
		//h = ({rrand(0.01, 1.0)}!20);
		h = ({1.0.linrand}!20);
		h.do {|n, j|
			//j.postln;
			f.write(n.asString ++ if (j<19, " ", Char.nl));
		};
		//f.write(Char.nl);
	};
	f.close;

	//x = FileReader.readInterpret("birds/avis-data-bird-" ++ m.asString ++ ".dat", true, true)
};
)

(
~ab_fitness = {|centr, flat, spread, flux|
	var ulim, llim, spmu, spsig, flmu, flsig, spreadr, fit;
	ulim = 1e6; llim = 800000;
	spmu = 0.3; spsig = 0.05;
	flmu = 0.005; flsig = 0.0004;
	// spread is centroid dependent
	spreadr = spread/(centr**2);
	fit = exp((((spreadr-spmu)**2)/(spsig**2*2)).neg);
	fit = fit +  exp((((flux-flmu)**2)/(flsig**2*2)).neg);
};
)

3200000/(4000**2)

(
~ab_mutate = {|a|
	var mpi = 0.4;  // mutation probability individual
	var mpg = 0.3; // mutation probability gene
	if(mpi.coin, {
		a.size.do{|i|
			if(mpg.coin, {
				//a[i] = rrand(0.01, 0.99);
				a[i] = 1.0.linrand;
				["changing " ++ i.asString].postln;
			}); // totally random
		};
	});
	a
};
)

// // test ab_mutate
// x = FileReader.readInterpret("birds/avis-data-bird-0-0.dat", true, true).flatten;
// x
// y = ~ab_mutate.value(x);
// y

(
~ab_crossover = {|a,b|
	var header, segsize, numseg, len, cut, ret;
	header = 6; segsize = 20; numseg = 7;
	len = numseg * segsize + header;
	cut = (header + ({|i| segsize * i}!7)).choose;
	if(0.5.coin, {
		ret = a[(0..(cut-1))] ++ b[(cut..(len-1))];
	}, {
		ret = b[(0..(cut-1))] ++ a[(cut..(len-1))];
	});
};
)

// // test ab_crossover
// x = FileReader.readInterpret("birds/avis-data-bird-0-0.dat", true, true).flatten;
// y = FileReader.readInterpret("birds/avis-data-bird-0-7.dat", true, true).flatten;
// x[(6..145)].plot
// y[(6..145)].plot
// z = ~ab_crossover.value(x, y);
// z[(6..145)].plot

//(1/((2*pi).sqrt*50000))
~ab_fitness.value(1,2,950000, 10);

( // prepare FFT buffer and listener channel
b = Buffer.alloc(s,2048,1);
~bbus = Bus.audio(s, 2);
)

(
20.do {|i|
	~ab_initbird.value(i); // initialize a bird file
};
)

//~ab_playbird.value(7);

x = [[1,2,3], [4,5,6,7]].flatten
x
x[(1..4)]

(
//~ab_playbird = {|birdnum|
// evolution section: loop generation, loop individuals, compute fitness, make new generation
20.do {|i|
	~ab_initbird.value(i); // initialize bird files
};
Task {
	var gen, ind, numgen, popsize;
	var pop, popn, selsize, genhead, seglen;
	var date, logf;
	gen = 0; numgen = 10;
	//fitness = Array.new(popsize); // init evolution data
	pop = Array.new(popsize);
	popsize = 20; selsize = 8;
	genhead = 6; // size of genome "header"
	seglen = 20; // segment length of functional genome partitions
	// init generation 0
	popsize.do {|i|
		// put genome into population
		pop = pop.add(FileReader.readInterpret("birds/avis-data-bird-" ++
			0.asString ++"-" ++ i.asString ++ ".dat", true, true).flatten);
	};
	// write evolution log
	date = Date.localtime;
	logf = File("birds/evolog_" ++ date.stamp.asString ++ ".dat", "w");

	numgen.do {|g|
		popsize.do {|i|
			var centr, crest, spread, flux, fitness;
			centr = 0; crest = 0; spread = 0; flux = 0; fitness = 0;
			["gen ", g, "iter ", i].postln;
			ind = g*popsize+i;
			// container

			~listener_resp = OSCresponder(nil, "/tr", {
				|t, r, m|
				["trignum ", m[2]].postln;
				if(m[2] == 100, {centr  = m[3];}); // select centroid
				if(m[2] == 101, {crest = m[3];}); // select crest
				if(m[2] == 102, {spread = m[3];}); // select spread
				if(m[2] == 103, {flux = m[3];}); // select flux
				if(m[2] == 200, {
					//r.postln;
					["centroid: ", (centr/m[3])].postln;
					["crest", (crest/m[3])].postln;
					["spread", (spread/m[3])].postln;
					["flux", (flux/m[3])].postln;
					fitness = ~ab_fitness.value(centr/m[3], crest/m[3], spread/m[3], flux/m[3]);
					["fitness ", fitness, "gen ", g, "ind ", i].postln;
					pop[popsize*g+i][0] = fitness;
					r.remove}); // remove responder when
				// synth terminates by sending duration in control blocks
			}).add;
			// 4 birds: high tweet, pinnng, hoot, kchrrr
			//( /* load data from file and fire up synth */
			0.1.wait;

			s.sendBundle(nil,
				["/s_new", "fofsynth3", 2000, 1, 0, \out, ~bbus.index,
				\amp, 0.1, \dur, pop[ind][1], \plvl, pop[ind][2], \pbias,
				pop[ind][3], \flvl, pop[ind][4], \fbias, pop[ind][5]], // start bird
				//["/n_setn", 2000, "penvd", 10] ++ Array.series(10, 1.0, -0.1) // set odd harmonics
				["/n_setn", 2000, "penv1d", 20] ++ pop[ind][((0*seglen+genhead)..(1*seglen+genhead-1))],
				["/n_setn", 2000, "penv2d", 20] ++ pop[ind][((1*seglen+genhead)..(2*seglen+genhead-1))],
				["/n_setn", 2000, "fenv1d", 20] ++ pop[ind][((2*seglen+genhead)..(3*seglen+genhead-1))],
				["/n_setn", 2000, "fenv2d", 20] ++ pop[ind][((3*seglen+genhead)..(4*seglen+genhead-1))],
				["/n_setn", 2000, "fenv3d", 20] ++ pop[ind][((4*seglen+genhead)..(5*seglen+genhead-1))],
				["/n_setn", 2000, "fenv4d", 20] ++ pop[ind][((5*seglen+genhead)..(6*seglen+genhead-1))],
				["/n_setn", 2000, "aenv1d", 20] ++ pop[ind][((6*seglen+genhead)..(7*seglen+genhead-1))],
				// listener synth
				["/s_new", "listener2", 2001, 1, 0, \amp, 1.0, \dur, pop[ind][1],
					\bufn, b.bufnum, \in, ~bbus.index]
				//	["/n_setn", 2000, "ring", 4] ++ Array.fill(4,0.1), // set shorter ring time
				//	["/n_setn", 2000, "amp", 4] ++ Array.fill(4,0.2) // set louder amps
			);
			(pop[ind][1] + 0.2).wait;
			//fitness.postln;

			// write individual to logf
			//pop.do {|p|
			pop[ind].do {|cell|
				logf.write(cell.asString ++ " "); // init fitness to 0
			};
			logf.write(Char.nl);
			//};

			pop.postln;

			// s.sendBundle(pop[ind][0] + 0.1,
			// 	["/n_free", 2001]);
			// s.sendBundle(0.09,
			// 	["/n_trace", 2000]);
			//)
			//};
		}; // end individual
		// start evolution operators here
		// 1 load generation into on 2D array
		//   done above already
		// 2 sort by fitness
		popn = pop[((g*popsize)..(g+1*popsize-1))]; // tmp
		popn.postln;
		popn.sort({|a,b| a[0] > b[0]});
		popn.postln;
		popn[0].postln;
		// 3 fill new generation
		// 3a with selection
		selsize.do {|i|
			pop = pop.add(popn[i]);
		};
		// 3b with crossover
		(popsize-selsize).do {|i|
			var ia, ib;
			var nind = i + selsize; // index of new individua;
			var range = (selsize..(popsize-1)); // breeding range
			ia = range.choose; // select ind a
			range.remove(ia); 
			ib = range.choose; // select ind b
			pop = pop.add(~ab_crossover.value(
				pop[g*popsize+ia],
				pop[g*popsize+ib]
			));
		};
		// 3c mutate individuals in new generation
		popsize.do {|i|
			pop[g+1*popsize+i] = ~ab_mutate.value(
				pop[g+1*popsize+i]
			);
		};
	}; // end generation
	// close logfile
	logf.close;
}.play; // end Task
)

////////////////////////////////////////////////////////////
// play logfiles, select actual birds
// logs with usable results
evolog_081118_165804.dat
evolog_081118_170905.dat // besonders am anfang
evolog_081118_171152.dat // knarzig, auch oben

(
// var filenum = ;
var filenums = ["081118_165804", "081118_170905", "081118_171152"];
f = Array.new(0);
filenums.do {|filenum|
	f = f.addAll(FileReader.readInterpret("birds/evolog_" ++ filenum ++ ".dat", true, true));
};
f.size.postln;

g = [f[221]];

Task {
	var seglen, genhead, synthid;
	seglen = 20;
	genhead = 6;
	g.do {|ind, i|
		["bird #", i].postln;
		synthid = 2000 + 0.rrand(100);
		s.sendBundle(nil,
			["/s_new", "fofsynth3", synthid, 1, 0, \out, 0,
				\amp, 0.1, \dur, ind[1], \plvl, ind[2], \pbias,
				ind[3], \flvl, ind[4], \fbias, ind[5]], // start bird
			["/n_setn", synthid, "penv1d", 20] ++ ind[((0*seglen+genhead)..(1*seglen+genhead-1))],
			["/n_setn", synthid, "penv2d", 20] ++ ind[((1*seglen+genhead)..(2*seglen+genhead-1))],
			["/n_setn", synthid, "fenv1d", 20] ++ ind[((2*seglen+genhead)..(3*seglen+genhead-1))],
			["/n_setn", synthid, "fenv2d", 20] ++ ind[((3*seglen+genhead)..(4*seglen+genhead-1))],
			["/n_setn", synthid, "fenv3d", 20] ++ ind[((4*seglen+genhead)..(5*seglen+genhead-1))],
			["/n_setn", synthid, "fenv4d", 20] ++ ind[((5*seglen+genhead)..(6*seglen+genhead-1))],
			["/n_setn", synthid, "aenv1d", 20] ++ ind[((6*seglen+genhead)..(7*seglen+genhead-1))]
		);
		(ind[1] + 0.3).wait;
	};
}.play;
)

////////////////////////////////////////////////////////////
// bird player
(

~world = Task {
	var birds, birdsel, birdctl;
	var filenums = ["081118_165804", "081118_170905", "081118_171152"];
	birds = Array.new(0);
	birdsel = [200, 201, 202, 203, 204, 212];
	// hack
	birdctl = [
		[0.2, 7.0, 11.0, [0.4]], // dur mod, wait mod range, XXX: integration table arbitrary distribution
		[0.05, 1.0, 1.5, [0.1, 3.0, 3.0]],
		[0.1, 5.0, 7.5, [0.4]],
		[1.0, 6.0, 8.5, [0.4]],
		[0.1, 7.0, 9.5, [0.4]],
		[0.1, 8.0, 10.5, [0.4]]
	]; // additional parameters for bird play loop
	filenums.do {|filenum|
		birds = birds.addAll(FileReader.readInterpret("birds/evolog_" ++ filenum ++ ".dat", true, true));
	};
	birds.size.postln;
	birds = birds[birdsel];
	birds.size.postln;
	birds.do {|ind, i|
		Task {
			var seglen, genhead, synthid;
			seglen = 20;
			genhead = 6;
			synthid = s.nextNodeID; // 2000 + 0.rrand(100);
			inf.do {
				s.sendBundle(nil,
					["/s_new", "fofsynth3", synthid, 0, 0, \out, 0,
						\amp, 0.1, \dur, ind[1] + birdctl[i][0].linrand, \plvl, ind[2], \pbias,
						ind[3], \flvl, ind[4], \fbias, ind[5], \pnoiselvl, 10, \fnoiselvl, 20], // start bird
					["/n_setn", synthid, "penv1d", 20] ++ ind[((0*seglen+genhead)..(1*seglen+genhead-1))],
					["/n_setn", synthid, "penv2d", 20] ++ ind[((1*seglen+genhead)..(2*seglen+genhead-1))],
					["/n_setn", synthid, "fenv1d", 20] ++ ind[((2*seglen+genhead)..(3*seglen+genhead-1))],
					["/n_setn", synthid, "fenv2d", 20] ++ ind[((3*seglen+genhead)..(4*seglen+genhead-1))],
					["/n_setn", synthid, "fenv3d", 20] ++ ind[((4*seglen+genhead)..(5*seglen+genhead-1))],
					["/n_setn", synthid, "fenv4d", 20] ++ ind[((5*seglen+genhead)..(6*seglen+genhead-1))],
					["/n_setn", synthid, "aenv1d", 20] ++ ind[((6*seglen+genhead)..(7*seglen+genhead-1))]
				);
				//(ind[1] + rrand(birdctl[i][1], birdctl[i][2])).wait;
				(birdctl[i][3].choose + rrand(birdctl[i][1], birdctl[i][2])).wait;
			};
		}.play;
	};
};
~world.play;
)

// j = JStethoscope.new(s, 2)
// j.numChannels_(2);
// j.run;

1000/21

(
{SinOsc.ar(1000/20.7, 0, 0.1) + 
	SinOsc.ar((1000/20.7)*3, 0, 0.1)
}.play;
)

(
{
	//Dust.ar(5, LFPulse.kr(5.reciprocal, 0, 0.2), 0)
	Impulse.ar(1, 0, LFPulse.kr(5.2.reciprocal, 0, 0.25), 0)
}.play;
)

///////////////////////////////////////////////////////////
// version 4
// fofsynth4, see rara-avis-synths.sc

//Complex(1.0, 1.0).rho

// test bird brain / modulation
(
SynthDef(\braintest1, {|out=0, amp=0.1, freq=1.0, thresh=0.5|
	var x1 = SinOsc.ar(freq, 0, 0.4);
	var x2 = SinOsc.ar(5.13*freq, 0, 0.2);
	var x3 = Trig1.ar(((x1+x2) > thresh) - 0.1, 1/freq);
	//	var x3 = (x1+x2) > thresh;
	Out.ar(out, [x1 + x2, x3] * amp);
}).store;
)
x = Synth(\braintest1, [\freq, 0.2]); // check in 2-chan scope
x.set(\thresh, 0.6);
x.set(\thresh, 0.58);
x.set(\thresh, 0.47);

//{Lag.kr(LFNoise0.kr(1), 0.2).poll}.play

{LFNoise0.ar(40)}.scope;

////////////////////////////////////////////////////////////
// version 5
// see rara-avis.sc


//Complex(-1, -0.001).theta
//(1-(Complex(-25.060812757344, 22.58570803243).rho/42.42)).squared

( // bird dict and brain-synth communication
// number of cells on each dimension
~width = 120;
~height = 120;
// birds storage
~birds_alive = Dictionary.new;
// setup source matrix
// source mean density
~source_prob = 10.squared.reciprocal;
// XXX: sources should have extension in space: center and surrounding
~source_mtx = Array.fill2D(~width, ~height, {if(~source_prob.coin, {(1..4).choose}, {0})});
// ~pos_resp = OSCresponder(nil, "/tr", {
// 	|t, r, m|
// 	if(m[2] == 201) {
// 		//m.postln;
// 		~birds_alive.at(m[1])[0] = m[3] + 1.0 / 2;
// 	};
// 	if(m[2] == 202) {
// 		//m.postln;
// 		~birds_alive.at(m[1])[1] = m[3] + 1.0 / 2;
// 	};
// });
// ~pos_resp.add;
)

// brain update here later


~mytriangle = [Point(0.0, 0.0), Point(1.0, 0.0), Point(cos(pi/3), sin(pi/3))];
~mytriangle * 3.0
~mytriangle.scale(3.0)
~mytriangle.translate(Point(1.0, 1.0))
~mytriangle +. Point(1.0, 1.0)

~win.close;


// pi - 2.0

//if((0.5.coin) && (0.5.coin), {"ok".postln;});
//true.and(false)

(
~world.pause;
s.freeAll;
~win.close;
)

// scoping
s.scope;
GUI.stethoscope.new(s, 2)
//JFreqScope.new

// recording
s.recHeaderFormat
s.recSampleFormat

s.recHeaderFormat_("wav");
s.recSampleFormat_("int16")
s.prepareForRecord
{
	s.record;
	60.0.wait;
	s.stopRecording;
}.fork;

// GUI experiments

//Rect(0, 0, 10, 13)

~blub = Dictionary.new;
~blub.add("blub" -> [0, 0])

~blub.at("blub")[0] = 1.0.rand;
~blub.at("blub")[1] = 1.0.rand2;
~blub.at("blub")

~blub.do {|a|
	a.postln;
}



//~lis = Synth.head(s, \listener2, [\in, ~bbus.index, \amp, 1.0, \bufn, b.bufnum]);
//~lis = Synth.head(s, \listener2, [\in, 0, \amp, 1.0, \bufn, b.bufnum]);


( // problem: use triggering
x = Synth(\fofsynth2, [\out, 0, \amp, 0.1, \dur, 3,
		//		{rrand(0, 1)}!20,
		//		{rrand(0, 2)}!20]
	\freqsb, [1000, 2000]
]).setn(\freqs, [
	//[0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1],
	[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
	[0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.9, 0.8, 0.7,
		0.6, 0.5, 0.4, 0.3, 0.2, 0.1]
]);
x.setn(\freqs, [
	//[0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1],
	[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
	[0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.9, 0.8, 0.7,
		0.6, 0.5, 0.4, 0.3, 0.2, 0.1]])
)

(
x = Synth(\fofsynth2, [
	\out, 0, \amp, 0.05, \dur, 2.0,
	\freqs, [
		{rrand(0.1, 1)}!20, // 20
		{rrand(0.1, 1)}!20], // 20
	\freqsl, [
		{rrand(1.0, 10.0)}!19, // 19
		{rrand(1.0, 10.0)}!19], // 19
	\freqsc, [
		({rrand(0, 4)}!20).neg, // 20
		({rrand(0, 4)}!20).neg], // 20
	\freqsb, [444, 777], // 2
	\ffreqs, {rrand(0.5, 1)}!20, // 20
	\ffreqsl, {rrand(1.0, 10.0)}!19,  // 19
	\ffreqsc, ({rrand(0, 4)}!20).neg,  // 20
	\ffreqsb, [345, 435, 948, 1240]  // 4
]);
)

s.boot;

// wasser, wind, zikaden, wildschweine

// evolution frame
// - listener synth

// control layer: movement, motivation, interaction

// external inputs

(1..1000).compile


// observer-private
1e-15
1/24e-15
// 41.6666 THz
3e9/(1/24e-15)
3e9/4.16e13

//infrared
780nm - 1mm
3e9/1e-6

// testing circular iteration
(
~test = {
	(1..4).do {|i|
		i.postln;
		[i, 0].postln;
		[0, i].postln;
		[i.neg, 0].postln;
		[0, i.neg].postln;
		(i-1).do {|j|
			j = j+1;
			[i, j].postln;
			[j, i].postln;
			[j.neg, i].postln;
			[i.neg, j].postln;
			[i.neg, j.neg].postln;
			[j.neg, i.neg].postln;
			[j, i.neg].postln;
			[i, j.neg].postln;
		};
		[i, i].postln;
		[i.neg, i].postln;
		[i.neg, i.neg].postln;
		[i, i.neg].postln;
	};
};
~test.value;
)

(0..0)

// new knowledge
// - while loops always use function
// - trunc: negative numbers get trunked to negative floor
~source_mtx[0][13.264911064067+30][-20.794733192202+30]
-3.2.trunc(0.5)
-3.2.floor
3.2.floor
