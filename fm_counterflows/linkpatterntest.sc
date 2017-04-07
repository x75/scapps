
Link.enable;

this.executeFile("../src/supercollider/scapps/world_synthdefs.sc");


(

~f_pwmseq = {|grid = 0, freq = 0.5, width = 0.5|
	var trigseq = LFPulse.kr(0.5, iphase: 0, width: 0.5);
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
  Out.ar(0, sig ! 2);
}).send(s);

)

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

z = Synth(\kik, [\amp, 2.0, \div, 1.0, \p1, 320]);
z = Synth(\kik, [\amp, 2.0, \div, 1/16, \p1, 240, \envdur, 2.0]);


Array.series(10, 0)
Array.geom(4, 1, 2).reverse.normalizeSum
