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
