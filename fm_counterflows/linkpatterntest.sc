
Link.enable;

this.executeFile("../src/supercollider/scapps/world_synthdefs.sc");


(

// CoinGate

~f_pwmseq = {|grid = 0, freq = 0.5, width = 0.5|
	var trigseq = LFPulse.kr(freq, iphase: 0, width: width);
	trigseq * grid
};

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

SynthDef(\kik, { |preamp = 1, amp = 1, dur = 0.1, envdur = 0.1, p1 = 200, p2 = 46, div = 1.0|
    var trig, trigseq, lane, rtrig;
	var sig, freq, freqenv;
	var ampenv;
	trig = LinkTrig.kr(div);
	// lane = Array.series(14, 0, step: 1);
	// lane = Array.geom(4, 1, 2);
	// trigseq = LinkLane.kr(div: 4.0, max: 16, lane: lane); // TDuty.kr(Dseq([], inf));
	// a = { Dseq([1, 2, 3, 4, 5], inf) };
	// trigseq = Demand.kr(trig, 0, Dwhite(0, 1, inf)) > 0.6;
	trigseq = TExpRand.kr(0.01, 1, trig) < 0.1;
	rtrig = trigseq * trig;
	freqenv = Env.new([p1 + TExpRand.kr(lo: 0.01, hi: 10, trig: rtrig), p2, p2, p1], [0.04, dur - 0.03, 0.01].normalizeSum(), -3);
	// freqenv.plot;
	freq = EnvGen.kr(freqenv, gate: rtrig, timeScale: dur,
		doneAction: 0);
	ampenv = EnvGen.kr(
		envelope: Env([0, 1, 0.8, 0], [0.01, 0.05, 0.1].normalizeSum()),
		gate: rtrig,
        timeScale: envdur,
		doneAction: 0);
	sig = SinOsc.ar(freq, 0.5pi, preamp).distort * amp * ampenv;
  Out.ar(0, sig ! 2);
}).send(s);

SynthDef(\snare909ish,{ |out=0, amp = 1,velocity=1, envdur = 0.1, div=1.0|
	var filtWhite;
    var grid, trig;
	grid = LinkTrig.kr(div);
	// trigseq = LFPulse.kr(0.5, iphase: 0, width: 0.5);
	trig = ~f_pwmseq.value(grid: grid, freq: ExpRand(lo: 0.2, hi: 2.0), width: Rand(lo: 0.2, hi: 0.8));
	filtWhite = LPF.ar(WhiteNoise.ar(1), 7040, 1) * (0.1 + velocity);
	Out.ar(out, (
		(
			/* Two simple enveloped oscillators represent the loudest resonances of the drum membranes */
			(LFTri.ar(330,0,1) * EnvGen.ar(Env.perc(0.0005,0.055), gate: trig, timeScale: 1.0, doneAction:0) * 0.25)
			+ (LFTri.ar(185,0,1) * EnvGen.ar(Env.perc(0.0005,0.075), gate: trig, timeScale: 1.0, doneAction:0) * 0.25)

			/* Filtered white noise represents the snare */
			//
			+ (filtWhite * EnvGen.ar(Env.perc(0.0005,0.4), gate: trig, timeScale: envdur, doneAction:0) * 0.2)
			+ (HPF.ar(filtWhite, 523, 1) * EnvGen.ar(Env.perc(0.0005,0.283), gate: trig, timeScale: envdur, doneAction:0) * 0.2)

		) * amp
	).dup(2)
	)
}).add;

SynthDef(\kickDrum, {
	|gate=0, vol=1, pos=0.0| //GPL licensed
	var daNoise,daOsc,env1,env2,env3;
	//noise filter cutoff envelope
	//controlls cutoff pitch...0 to 80 Hz
	env1=Env.perc(0.001,1,80,-20);
	//mix-amp envelope
	//controlls overall amplitude...0 to 1
	env2=Env.perc(0.001,1,vol,-8);
	//osc-pitch envelope
	//controlls pitch of the oscillator...0 to 80 Hz
	env3=Env.perc(0.001,1,80,-8);
	//Attack noise portion of the sound
	//filter cutoff controlled by env1
	//+20 to move it into the audible
	//spectrum

	daNoise=LPF.ar(WhiteNoise.ar(1),EnvGen.kr(env1,gate)+20);

    //VCO portion of the sound
    //Osc pitch controlled by env3
    //+20 to move the pitch into the
    //audible spectrum
	daOsc=LPF.ar(SinOsc.ar(EnvGen.kr(env3,gate)+20),200);

	//output
	Out.ar(0,PanB2.ar(
		Mix([daNoise,daOsc]) * EnvGen.kr(env2,gate,doneAction: 2),
		pos //position
		//level controlled by env2
	);
	);
}).add;


)

{BrownNoise.kr()}.freqscope

(
100.do({|i|
	i.postln;
	Synth(\hihat, [\amp, 0.01.exprand(0.2)]);
});
)

x = Synth(\hihat, [\amp, 1.0]);
x = Synth(\hihat, [\amp, 0.4]);
x = Synth(\hihat, [\amp, 0.1]);
x = Synth(\hihat, [\amp, 0.3, \div, 8.0]);

y = Synth(\snare, [\amp, 0.8, \div, 1.0]);
y = Synth(\snare, [\amp, 0.6, \div, 0.5]);
y = Synth(\snare909ish, [\amp, 2.2, \div, 1.0, \envdur, 0.35]);


z = Synth(\kik, [\amp, 4.0, \div, 2.0, \p1, 320, \dur, 0.05, \envdur, 0.05]);
z = Synth(\kik, [\amp, 2.0, \div, 0.125, \p1, 240, \dur, 2.0, \envdur, 2.0]);


Array.series(10, 0)
Array.geom(4, 1, 2).reverse.normalizeSum
