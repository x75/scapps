// rara avis
// (c) oswald berthold 2008, 2009

(
SynthDef(\noise, {
	|outbus=0, amp=1.0|
	Out.ar(outbus, WhiteNoise.ar(amp));
}).store;

// THE water
SynthDef(\water_f, { // water_filter XXX
	|noisebus=0, outc=0, dens=30, fwidth=1000, foffs=60, fq=0.001, amp=0.1, pos=0.0|
	var out, rfreq;
	// noise = WhiteNoise.ar(SinOsc.ar(0.5, 0, 0.01, 0.98), 0.0);
	rfreq = Latch.ar(WhiteNoise.ar, Dust.ar(dens));
	out = Resonz.ar(In.ar(noisebus, 1), abs(Lag.ar(rfreq * (fwidth + foffs) + foffs), fq), 0.02, amp);
	Out.ar(0, PanB2.ar(out, pos));
}).store;

// low-passed filtered noise
SynthDef(\water_t, {
	|out=0, size = 1, ffreq=100.0, fq=0.1, amp=1.0|
	var noise, outs, rfreq;
	noise = WhiteNoise.ar(0.2, 0.0);
	// rfreq = Latch.ar(WhiteNoise.ar, Impulse.ar(7.3));
	// rfreq = Latch.ar(WhiteNoise.ar, Dust.ar(60.6));
	outs = { Resonz.ar(noise, ffreq + LFDNoise1.ar(13.3, ffreq*0.1), fq, amp) }!2;
	Out.ar(out, outs);
}).store;


// stream of water
SynthDef(\stream5, {|amp=1.0, out=0, az=0.11|
	var modfreqs = [[3, 1600, 800], [3, 1200, 600], [5, 1800, 900], [5, 2200, 1100]];
	var bpfreq = [1980, 1700];
	var freq = {|i| 
		Lag.kr(LFNoise0.kr(1000/modfreqs[i][0], modfreqs[i][1], modfreqs[i][2]), 0.011)
	}!4;
	var freqd = LPF.kr(freq - Delay1.kr(freq), 10);
	var osc = SinOsc.ar(freq, 0, freqd.squared * 0.001);
	var oscaux1 = BPF.ar(osc[0] * osc[1] * 0.2, 1000, 0.125);
	var oscaux2 = BPF.ar(osc[2] * osc[3] * 0.2, 2000, 0.125);
	// {|i| BPF.ar(osc[i*2] + osc[i*2+1] + oscaux[i], bpfreq[i], 0.5, -72.dbamp) }!2;
	var hpf1 = HPF.ar((osc[0] * 0.04) + (osc[1]*0.05) + oscaux1, 600);
	var hpf2 = HPF.ar((osc[2] * 0.1)  + (osc[3]*0.1)  + oscaux2, 700);
	var bpfaux = HPF.ar(BPF.ar(hpf1 * hpf2, 700, 0.125), 1000, 2);
	var fout = HPF.ar(hpf1 * 0.05 + (hpf2 * 0.009) + bpfaux, 800, 20);
	//fout.poll;
	//Out.ar(0, fout.dup * amp);
	Out.ar(out, PanB2.ar(fout, az, amp));
}).store;

////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////
// world, version 2
// version 4: env arrays as controls, synth with brain
SynthDef(\fofsynth4, {|out=0, amp= 0.707, dur=1.0, plvl=1000.0,
	pbias=1000.0, flvl=4000.0, fbias=0.0, pnoiselvl = 0.0,
	fnoiselvl = 0.0, callfreq=0.1, calldrift=1.0, callduty=1.0, callmodfreq=0,
	az = 0, gn = 1.0|
	// voice vars
	var sig, penv, penvg, fenv, fenvg, aenv, aenvg;
	var numform = 4;
	var envelems = 20;
	// position
	var birdx, birdy, xmodf, ymodf;
	// brain vars
	var mcycle;
	var trig, callmod, x1, x2, x3;
	// voice trig, position, internal stores, 
	// modulations
	dur = dur + LFNoise1.kr(1.13, 0.05, -0.025);
	// modl 1
// 	mcycle = SinOsc.kr(LFNoise1.kr(5.reciprocal, mul: 20.reciprocal, add: 5.reciprocal));
// 	trig = Trig.kr(mcycle > 0.9, 0.1); // Impulse.kr(0);
	// modl 2
	callmod = LFNoise0.kr(callmodfreq, 1.0, -0.5);
// 	x1 = Impulse.kr(callfreq*calldrift, phase: Rand(0.0, 1.0), add: -0.1);
// 	x2 = LFPulse.kr(callfreq, 0, callduty);
// 	trig = x1 * x2;
	// modl 3
	x1 = SinOsc.kr(callfreq, Rand(0.0, 1.0), 1.0);
	x2 = SinOsc.kr(calldrift+callmod*callfreq, 0, 1.0);
	trig = Trig1.kr((x1+x2) > (callduty*2), 1000.reciprocal);
	//	var x3 = (x1+x2) > thresh;
	// finally: use Select or Mix to change modulation dependent on
	// location in 3-space (nest, drinking, prey, ...)
	// voice part
	// instrument
	penv = {|i|
		Env(
			Control.names(["penv" ++ (i+1).asString ++ "d"]).ir(Array.series(envelems,0,0.1)),
			Control.names(["penv" ++ (i+1).asString ++ "t"]).ir(Array.series(envelems-1,1,0).normalizeSum),
			Control.names(["penv" ++ (i+1).asString ++ "c"]).ir(Array.series(envelems,-1,0))
		)} ! 2;
	penvg = {|i| EnvGen.kr(penv[i], trig, plvl, pbias, dur)}!2;
	sig = Blip.ar(penvg + LFNoise1.kr(10, pnoiselvl), 40, 0.4);
	fenv = {|i|
 		Env(
			Control.names(["fenv" ++ (i+1).asString ++ "d"]).ir(Array.series(envelems,0,0.1)),
			Control.names(["fenv" ++ (i+1).asString ++ "t"]).ir(Array.series(envelems-1,1,0).normalizeSum),
			Control.names(["fenv" ++ (i+1).asString ++ "c"]).ir(Array.series(envelems,-1,0))
		)} ! numform;
	fenvg = {|i| EnvGen.kr(fenv[i], trig, flvl, fbias, dur) } ! numform;
	sig = Formlet.ar(sig, fenvg + LFNoise1.kr(10, fnoiselvl) /*Line.ar(1, ffreqs, dur)*/, 0.01, 0.05);
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
	aenvg = EnvGen.kr(aenv[0], trig, amp, 0.0, dur, doneAction: 0);
	//Out.ar(out, Mix(sig*aenvg).dup);
// 	// inline brain / disabled 20090127
//	xmodf = 0.021; ymodf = 0.0273;
// 	birdx = Lag.kr(DelayN.kr(LFNoise0.kr(xmodf), xmodf.reciprocal, Rand(xmodf.reciprocal)), 1.97); // 2.5); // should be some brownian type process
// 	birdy = Lag.kr(DelayN.kr(LFNoise0.kr(ymodf), ymodf.reciprocal, Rand(ymodf.reciprocal)), 2.0);
// 	SendTrig.kr(Impulse.kr(10), 201, birdx);
// 	SendTrig.kr(Impulse.kr(10), 202, birdy);
// 	Out.ar(out, PanB2.ar(Mix(sig*aenvg), azimuth: Complex(birdx, birdy).theta,
// 		gain: (1-(Complex(birdx, birdy).rho/1.4142135623731)).squared //Lag.kr(LFNoise0.kr(0.1, 0.5, 0.5), 0.2)
// 	));
// end inline brain
	Out.ar(out, PanB2.ar(Mix(sig*aenvg), azimuth: az, gain: gn));
// 	Out.ar(0, K2A.ar([x1 + x2, trig]) * amp);
}).store;
)
