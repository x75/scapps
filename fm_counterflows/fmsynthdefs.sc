// main output mixer
~makeSynthDef = {
	arg name, busnum = 10;
	SynthDef(name, {
		arg out = 0, amp = 1.0, busoffset = 4;
		var busmix;
		busmix = Mix.fill(busnum,
			{|i| Pan2.ar(In.ar(busoffset+i), -1 + (i*(2/busnum)))})
		* busnum.reciprocal;

		// amp_env = EnvGen.ar(Env.asr(0.005,1,0.005,[2,-2]), gate: gate,
		// doneAction: 2);
		// mixsig = Mix.fill(sines,
		// {SinOsc.ar((pitch+(Rand(0.0,1.0).pow(1.5)*24)).midicps)})
		// * sines.reciprocal;
		// sig = mixsig * amp_env * amp;

		Out.ar(out,busmix);
	})
};
~makeSynthDef.value(\fmmixer,10).send;

// generator defs
SynthDef(\fm1, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cellinval = In.kr(cellin);
	//var s1 = SinOsc.ar(Lag.kr(cellinval + LFNoise2.kr(10, mul: 20, add: 0), 0.001));
	var s1 = SinOsc.ar(Lag.kr(cellinval * LFNoise2.kr(10, mul: 20, add: 0), 0.001), cellinval);
	var f1 = Pitch.kr(s1);
	// Poll.kr(Impulse.kr(2.0), f1, label: \u ++ NodeID.ir.poll);
	Out.kr(cellout, f1);
	Out.ar(out, s1);
	// SinOsc.ar({134.0.rand2(137.0)} ! 4, Lag.kr(Pitch.kr(~out.ar(4).reverse) * 0.1 * LFNoise2.kr(0.5, 4pi * 0.9), 0.1), 0.3) }
}).send(s);

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
	var sig = SinOsc.ar(freq: [60, 60.7] * cellin.abs,               phase: LocalIn.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.1405);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
SynthDef(\chaosfb3, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var sig = SinOsc.ar(freq: Pitch.kr(LocalIn.ar(2)),     phase: LocalIn.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.2);
	LocalOut.ar(sig);
	Out.kr(cellout, Pitch.kr(sig)/10000.0 - 1);
	Out.ar(out, sig);
}).send(s);
// SynthDef(\chaosfb4, {
// 	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
// 	var sig = SinOscFB.ar(freq: Pitch.kr(~out.ar(2)),   feedback: ~out.ar(2).reverse * LFNoise2.kr(0.5, 4pi), mul: 0.2);
// 	Out.ar(out, sig);
// }).send(s);
// SynthDef(\chaosfb5, {
// 	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
// 	var sig = SinOsc.ar(freq: {100.0.rand2(300.0)} ! 4, phase: ~out.ar(4).distort.reverse.tanh * LFNoise2.kr(0.5, 4pi * 0.8), mul: 0.2);
// 	Out.ar(out, sig);
// }).send(s);
// SynthDef(\chaosfb6, {
// 	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
// 	var sig = SinOsc.ar(freq:{134.0.rand2(137.0)} ! 4,  phase: Lag.kr(Pitch.kr(~out.ar(4).reverse) * 0.1 * LFNoise2.kr(0.5, 4pi * 0.9), 0.1), mul: 0.3);
// 	Out.ar(out, sig);
// }).send(s);
