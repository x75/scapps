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

~f_cout_pitch_raw = {
	|cellout, sig|
	(Pitch.kr(sig)/(SampleRate.ir/2)) - 1;
};

~f_get_cin = {
	|cellin = 0|
	var wrap, cin;
	wrap = Rand(lo: 0.0, hi: 1.0) > 0.3;
	cin = (wrap * Wrap.kr(In.kr(cellin), lo: -1.5, hi: 1.5)) + ((1 - wrap) * In.kr(cellin));
	cin
};

~f_lf_ens_1 = {
	var lfs = [];
	10.do({
		lfs = lfs.add(SinOsc.kr(ExpRand(lo: 0.033, hi: 0.3), mul: 3.0).abs.clip(0, 1));
	});
	lfs.product;
};

// make it sparser, hard lfo pulses
~f_lf_ens_2 = {
	var lfs = [];
	10.do({
		var freq = TExpRand.kr(lo: 0.05, hi: 1.0, trig: Impulse.kr(0));
		var duty = 0.9 - (freq * 0.5);
		// lfs = lfs.add(SinOsc.kr(ExpRand(lo: 0.033, hi: 0.3), mul: 3.0).clip(-1, 1));
		lfs = lfs.add(Lag3.kr(LFPulse.kr(freq,
			width: 0.1, //duty,
			mul: 1.0).clip(0, 1)), 0.2);
	});
	lfs.sum;
};


// synthdef wrapper
~makeSynthDefWrapper = {
	|name = \bla, synthfunc = nil|
	SynthDef(name, {
		|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0, gate = 1|
		var cin, freq, fret, sout, cout;
		var fargs, myf;
		var menv;
		cin = ~f_get_cin.value(cellin);
		freq = {ExpRand(180.0, 400.0)} ! 2;
		sout = SinOsc.ar(100);
		// sig = synthfunc.value();
		// fargs = thisFunction.def.varArgs;//makeEnvirFromArgs;
		// ["fargs", fargs].postln;
		// myf = synthfunc.def.sourceCode.asCompileString.compile;//.value(fargs);
		// myf.value(1);
		fret = synthfunc.value(in: in, out: out, amp: amp, cellin: cellin, cellout: cellout, cin: cin, freq: freq);
		sout = fret[0];
		cout = fret[1];
		menv = EnvGen.kr(
			envelope: Env.asr(attackTime: 0.1, sustainLevel: 1.0, releaseTime: 1.0, curve: -4),
			gate: gate, doneAction: 2);
		LocalOut.ar(sout);
		Out.kr(cellout, cout);
		Out.ar(out, sout * menv);
	});
};

~makeSynthDefWrapper.value(\blu, {
	// arg ... args;
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0, cin = 0, freq = 0|
	var sout, cout;
	sout = SinOsc.ar(ExpRand(lo: 60, hi: 1000));
	cout = (Pitch.kr(sout)/SampleRate.ir/2) - 1;
	[sout, cout]
}).send;

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