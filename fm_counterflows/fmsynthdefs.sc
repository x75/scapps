// shared synths

// main output mixer with wrapper
~makeSynthDef = {
	arg name, busnum = 10;
	var changains = Array.fill(busnum, {|i| busnum.reciprocal });
	// changains.postln;
	SynthDef(name, {
		arg out = 0, amp = 1.0, busoffset = 4; //, changain = changains;
		var busmix;
		busmix = Mix.fill(
			n: busnum,
			function: {|i|
				Pan2.ar(In.ar(busoffset+i), -1 + (i*(2/busnum)))
			}
		);
		// amp_env = EnvGen.ar(Env.asr(0.005,1,0.005,[2,-2]), gate: gate,
		// doneAction: 2);
		// mixsig = Mix.fill(sines,
		// {SinOsc.ar((pitch+(Rand(0.0,1.0).pow(1.5)*24)).midicps)})
		// * sines.reciprocal;
		// sig = mixsig * amp_env * amp;

		Out.ar(out,busmix * busnum.reciprocal);
		// Out.ar(out, In.ar(busoffset, busnum) * changain);
	});
};
~makeSynthDef.value(\fmmixer,10).send;

// util funcs
~f_cout_pitch = {
	|cellout, sig|
	Out.kr(cellout, (Pitch.kr(sig)/(SampleRate.ir/2)) - 1);
};

~f_get_cin = {
	|cellin = 0|
	var wrap, cin;
	wrap = Rand(lo: 0.0, hi: 1.0) > 0.3;
	cin = (wrap * Wrap.kr(In.kr(cellin), lo: -1.5, hi: 1.5)) + ((1 - wrap) * In.kr(cellin));
	cin
};

// // synthdef wrapper
// ~makeSynthDefWrapper = {
// 	|name = \bla, synthfunc = nil|
// 	SynthDef(name, {
// 		|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
// 		var cin, freq, sig;
// 		var fargs, myf;
// 		cin = ~f_get_cin.value(cellin);
// 		freq = {ExpRand(180.0, 400.0)} ! 2;
// 		sig = SinOsc.ar();
// 		// sig = synthfunc.value();
// 		fargs = thisFunction.def.varArgs;//makeEnvirFromArgs;
// 		["fargs", fargs].postln;
// 		myf = synthfunc.def.sourceCode.asCompileString.compile;//.value(fargs);
// 		myf.value(1);
// 		LocalOut.ar(sig);
// 		Out.kr(cellout, (Pitch.kr(sig)/SampleRate.ir/2) - 1);
// 		Out.ar(out, sig);
// 	}).send(s);
// };
//
// // ~makeSynthDefWrapper.def.sourceCode.asCompileString.compile
//
// ~makeSynthDefWrapper.value(\blu, {
// 	arg ... args;
// 	["args", args].postln;
// 	// cin.postcs
// 	// sig = SinOsc.ar(freq) ! 2;
// });
//
// (
// var myf;
// ~f1 = {
// 	|f3 = nil|
// 	var bla, f2;
// 	bla = 10.rand;
// 	f2 = {
// 		["hello", bla].postln;
// 	};
// 	f2.value;
// 	// thisFunction.valueEnvir.postln;
// 	f3.value(bla, thisFunctionDef.makeEnvirFromArgs);
// };
// // myf = {["context", bla].postln};
// ~f1.value(f3: {|bla = nil ... args| ["context", bla].postln; ["args", args].postln; nil})
// // {["context", bla].postln}.perform
// // ~f1.perform(\myf);
// )
//
// "".perform(\a)
//
// x = Synth(\blu);

// reference generator defs
SynthDef(\fm1, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cellinval = In.kr(cellin);
	//var s1 = SinOsc.ar(Lag.kr(cellinval + LFNoise2.kr(10, mul: 20, add: 0), 0.001));
	// var s1 = SinOsc.ar(Lag.kr((cellinval + 1) * ExpRand(50.0, 200.0) * LFNoise2.kr(10, mul: 20, add: 0), 0.001), cellinval);
	var s1 = SinOsc.ar(
		freq: Lag.kr((cellinval + 1) * LFNoise2.kr(10, mul: 20, add: 0), 0.001),
		phase: cellinval
	);
	// var f1 = Pitch.kr(s1);
	Poll.kr(Impulse.kr(1.0), cellinval, \cellinval);
	// Poll.kr(Impulse.kr(2.0), f1, label: \u ++ NodeID.ir.poll);
	// Out.kr(cellout, (Pitch.kr(s1)/SampleRate.ir/2) - 1);
	~f_cout_pitch.value(cellout, s1);
	// Out.kr(cellout, f1);
	Out.ar(out, s1);
	// SinOsc.ar({134.0.rand2(137.0)} ! 4, Lag.kr(Pitch.kr(~out.ar(4).reverse) * 0.1 * LFNoise2.kr(0.5, 4pi * 0.9), 0.1), 0.3) }
}).send(s);


// {SinOsc.ar([100, 120]) * [0.1, 0.5]}.scope