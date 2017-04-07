// original version from jitlib examples
SynthDef(\chaosfb1, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cin = In.kr(cellin);
	var sig = SinOsc.ar(freq: [220.0, 360.0],               phase: LocalIn.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.3);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);

// olly's synth
SynthDef(\chaosfb2, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cin = In.kr(cellin);
	var sig = SinOsc.ar(freq: [60, 60.7] * cin.abs ,               phase: LocalIn.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.1405);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
SynthDef(\chaosfb3, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cin = In.kr(cellin);
	var sig = SinOsc.ar(freq: Pitch.kr(LocalIn.ar(2)),     phase: LocalIn.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.2);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
SynthDef(\chaosfb4, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cin = In.kr(cellin);
	var sig = SinOscFB.ar(freq: Pitch.kr(LocalIn.ar(2)),   feedback: LocalIn.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.2);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
SynthDef(\chaosfb5, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cin = In.kr(cellin);
	var sig = SinOsc.ar(freq: {100.0.rand2(300.0)} ! 4, phase: LocalIn.ar(4).distort.reverse.tanh * LFNoise2.kr(0.5, 4pi * 0.8), mul: 0.2);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
SynthDef(\chaosfb6, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cin = In.kr(cellin);
	var sig = SinOsc.ar(freq:{134.0.rand2(137.0)} ! 4,  phase: Lag.kr(Pitch.kr(LocalIn.ar(4).reverse) * 0.1 * LFNoise2.kr(0.5, 4pi * 0.9), 0.1), mul: 0.3);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
//

SynthDef(\specen, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var ein, fft, entropy;

	var cin, sig, entf;

	//in = SinOsc.ar(MouseX.kr(100,1000),0,0.1);
	// in = Mix(SinOsc.ar([440,MouseX.kr(440,880)],0,0.1));
	// ein = SoundIn.ar;
	// ein = ein;// * LFPulse.ar(freq: 0.5);
	// ein = ein + (LocalIn.ar(2).reverse * 0.1);
	ein = In.ar(in) + WhiteNoise.ar(mul: 1e-6);
	fft = FFT(LocalBuf(4096), ein);

	entropy = SpectralEntropy.kr(fft,4096,2);    //one output band (so full spectrum's entropy)

	cin = In.kr(cellin);
	// LFNoise2.kr(0.5, 4pi)
	entf = Lag.kr(entropy, lagTime: ExpRand(1.0, 10.0));// * LFNoise2.kr(freq: 0.1, mul: 1.0);
	// [220.0, 360.0]
	sig = SinOsc.ar(freq: [ExpRand(40.0, 100.0), ExpRand(40.0, 100.0)] * entf * 1.0,               phase: LocalIn.ar(2).reverse * entf * 1.0, mul: 0.3);
	// sig = SMS.ar(ein, 80, MouseY.kr(1,50), 8, 0.3);
	// + WhiteNoise.kr(mul: 0.05)
	// entropy.poll;
	// ein.poll;
	Poll.kr(Impulse.kr(1/4.0), entropy, \specen);

	LocalOut.ar(sig);
	Out.kr(cellout, (entf * 0.0025) - 1.0);
	Out.ar(out, sig);
	// Out.ar(0,Pan2.ar(0.1*Blip.ar(100,10*(entropy.sqrt))));
}).send(s);

SynthDef(\sensdiss, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var ein, fft, entropy;

	var cin, sig, entf;

	//in = SinOsc.ar(MouseX.kr(100,1000),0,0.1);
	// in = Mix(SinOsc.ar([440,MouseX.kr(440,880)],0,0.1));
	// ein = SoundIn.ar;
	// ein = ein;// * LFPulse.ar(freq: 0.5);
	// ein = ein + (LocalIn.ar(2).reverse * 0.1);
	ein = In.ar(in) + WhiteNoise.ar(mul: 1e-6);
	fft = FFT(LocalBuf(8192), ein);

	entropy = SensoryDissonance.kr(fft: fft, maxpeaks: 100, peakthreshold: 0.001, norm: 10.0);    //one output band (so full spectrum's entropy)

	cin = In.kr(cellin);
	// LFNoise2.kr(0.5, 4pi)
	entf = Lag.kr(entropy, lagTime: ExpRand(1.0, 10.0));// * LFNoise2.kr(freq: 0.1, mul: 1.0);
	sig = SinOsc.ar(freq: [ExpRand(40.0, 100.0), ExpRand(40.0, 100.0)] * entf * 1.0 + 40,               phase: LocalIn.ar(2).reverse * entf * 1.0, mul: 0.3);
	// sig = SMS.ar(ein, 80, MouseY.kr(1,50), 8, 0.3);
	// + WhiteNoise.kr(mul: 0.05)
	Poll.kr(Impulse.kr(1/4.0), entf, \sensdiss);
	// ein.poll;

	LocalOut.ar(sig);
	Out.kr(cellout, (entf * -2.0) - 1);
	Out.ar(out, sig);
	// Out.ar(0,Pan2.ar(0.1*Blip.ar(100,10*(entropy.sqrt))));
}).send(s);

~f_pwmseq = {|grid = 0, freq = 0.5, width = 0.5|
	var trigseq = LFPulse.kr(0.5, iphase: 0, width: 0.5);
	trigseq * grid
};

// beat foo

SynthDef(\kik, { |in = 0, out = 0, preamp = 1, amp = 1, dur = 0.1, envdur = 0.1, p1 = 200, p2 = 46, div = 1.0, cellin = 0, cellout = 0|
    var trig, trigseq, lane, rtrig;
	var sig, freq;
	trig = LinkTrig.kr(div);
	lane = Array.series(14, 0, step: 1);
	lane = Array.geom(4, 1, 2);
	trigseq = LinkLane.kr(div: 4.0, max: 16, lane: lane); // TDuty.kr(Dseq([], inf));
	// a = { Dseq([1, 2, 3, 4, 5], inf) };
	// trigseq = Demand.kr(trig, 0, Dwhite(0, 1, inf)) > 0.6;
	trigseq = TExpRand.kr(0.01, 1, trig) < 0.1;
	rtrig = trigseq * trig;
	freq = EnvGen.kr(Env([p1, p2, p2, p1], [0.04, dur - 0.03, 0.01], -3), gate: rtrig, doneAction: 0);
	sig = SinOsc.ar(freq, 0.5pi, preamp).distort * amp * EnvGen.kr(Env([0, 1, 0.8, 0], [0.01, 0.05, 0.1].normalizeSum() * envdur),
		gate: rtrig,
		doneAction: 0);
  Out.ar(out, sig ! 2);
}).send(s);

SynthDef(\hihat, {| in = 0, out = 0, amp = 1.0, div = 4.0, len = 0.01, cellin = 0, cellout = 0|
	var ns, sig;
    var grid, trig;
	// trig = Impulse.kr(2.43);
	// lane = Array.series(14, 0, step: 1);
	// lane = Array.geom(4, 1, 2);
	// trigseq = LinkLane.kr(div: 4.0, max: 16, lane: lane); // TDuty.kr(Dseq([], inf));
	// a = { Dseq([1, 2, 3, 4, 5], inf) };
	// trigseq = Demand.kr(trig, 0, Dwhite(0, 1, inf)) > 0.6;
	// trigseq = TExpRand.kr(0.01, 1, trig) < 0.1;
	grid = LinkTrig.kr(div);
	// trigseq = LFPulse.kr(0.5, iphase: 0, width: 0.5);
	trig = ~f_pwmseq.value(grid: grid, freq: ExpRand(lo: 0.2, hi: 2.0), width: Rand(lo: 0.2, hi: 0.8));
	// trigseq = Demand.kr(trig, 0, Dwrand([0, 1], [0.7, 0.3], inf));
	sig = Linen.kr(gate: trig, attackTime: 0.01, susLevel: amp, releaseTime: len,
		doneAction: 0);
	ns = WhiteNoise.ar(sig);
    Out.ar(out, (ns * amp) ! 2)
}).send(s);

SynthDef(\snare, {| in = 0, out = 0, len = 0.1, amp = 1.0, div = 1.0, freq = 160, cellin = 0, cellout = 0|
	var ns, sig, tone;
	var trig, trigseq, lane;
	trig = LinkTrig.kr(div);
	lane = Array.series(14, 0, step: 1);
	lane = Array.geom(4, 1, 2);
	trigseq = LinkLane.kr(div: 4.0, max: 16, lane: lane); // TDuty.kr(Dseq([], inf));
	// a = { Dseq([1, 2, 3, 4, 5], inf) };
	// trigseq = Demand.kr(trig, 0, Dwhite(0, 1, inf)) > 0.6;
	trigseq = TExpRand.kr(0.01, 1, trig) < 0.1;
	sig = Linen.kr(gate: trigseq * trig, attackTime: 0.01, susLevel: 1, releaseTime: len, doneAction: 0);
	tone = SinOsc.ar(freq, 0.0, 0.3 * sig);
	ns = WhiteNoise.ar(sig);
	Out.ar(out, (amp * (0.5 * ns + tone)) ! 2)
}).send(s);
