
// init once
(
// global storage
g = Dictionary.newFrom([
	\gen, Dictionary.new(n: 100),
	\mes, Dictionary.new(n: 100),
	\net, Dictionary.new(n: 100),
	\dat, Dictionary.new(n: 100),
	\bus, Dictionary.new(n: 100),
	\buf, Dictionary.new(n: 100),
]);

// fft
g[\buf].put(\fft, Buffer.alloc(s, 2**12, 1));

// // measurement
// g[\bus].put(\mes_amp, Bus.control(s, numChannels: 1));
// trigger
g[\bus].put(\trig_sync, Bus.control(s, numChannels: 1));
// // params
// g[\bus].put(Bus.control(s, numChannels: 1);
)

(
SynthDef(\sin_freq2, {|freq(100), active(0)|
	var carrier;
	// carrier = SinOsc.ar(freq: freq, phase: 0, mul: 1, add: 0);
	// carrier = SinOsc.ar(freq: TRand.kr(100, 200, Impulse.kr(1)), phase: 0, mul: 3.0, add: 0);
	carrier = SinOsc.ar(freq: freq, phase: 0, mul: 1.0, add: 0);
	Out.ar(0, carrier * active);
}).add;

SynthDef(\trig, {|freq(1), active(0), trigbus|
	var trig;
	// carrier = SinOsc.ar(freq: freq, phase: 0, mul: 1, add: 0);
	// carrier = SinOsc.ar(freq: TRand.kr(100, 200, Impulse.kr(1)), phase: 0, mul: 3.0, add: 0);
	trig = Impulse.kr(freq);
	Out.kr(trigbus, trig);
}).add;

SynthDef(\meas_amp, {|active(0), trigbus(0)|
	var in, chain, trig_l;
	in = SoundIn.ar;
	// trig_l = Trig.kr(Impulse.kr(10));
	trig_l = Trig.kr(In.kr(trigbus));
	// a = AverageOutput.kr(in, Impulse.kr(1));
	// a = Median.kr(length: 31, in: Amplitude.kr(in));
	chain = FFT(c.bufnum, LeakDC.ar(in));
	a = FFTPower.kr(chain);
	// a = SpectralEntropy.kr(chain,2**12,1);
	// a.poll;
	SendTrig.kr(trig_l, id: 10, value: a * active);
	Out.kr(b, a * active);
}).add;

SynthDef(\meas_se, {|active(0), trigbus(0)|
	var in, chain, trig_l;
	in = SoundIn.ar;
	// trig_l = Trig.kr(Impulse.kr(10));
	trig_l = Trig.kr(In.kr(trigbus));
	// a = AverageOutput.kr(in, Impulse.kr(1));
	// a = Median.kr(length: 31, in: Amplitude.kr(in));
	chain = FFT(c.bufnum, in);
	// a = FFTPower.kr(chain);
	a = SpectralEntropy.kr(chain,2**12,1);
	// a.poll;
	SendTrig.kr(trig_l, id: 11, value: a * active);
	Out.kr(b, a * active);
}).add;
)

g
n
g[\net][\1]
g[\net][\o1].free


(

g[\dat].put(1, List.new());
// g[\dat].put(\2, List.new());
g[\net].put(1, NetAddr("127.0.0.1", 5005));

g[\gen].put(1, Synth(\sin_freq2, [\freq, 440]));
g[\mes].put(1, Synth(\meas_amp, []));
g[\mes].put(2, Synth(\meas_se, []));
// x = Synth(\searcher1, []);

// register to receive this message
g[\net].put(\o1, OSCFunc({ arg msg, time;
	// [time, msg].postln;
	n.sendMsg(\tr, msg[0], msg[1], msg[2], msg[3]);
	// // l.add([time, msg]);
	// if(msg[2] == 10, {
	// 	n.sendMsg(msg);
	// }, nil);
},'/tr', s.addr));

g[\net].put(\o2, OSCFunc({ arg msg, time;
	[time, msg].postln;
	g[\gen][msg[1]].set(msg[2], msg[3]);
},'/gen'));

g[\net].put(\o3, OSCFunc({ arg msg, time;
	[time, msg].postln;
	g[\mes][msg[1]].postln;
	g[\mes][msg[1]].set(msg[2], msg[3]);
},'/mes'));

g[\net].put(\o4, OSCFunc({ arg msg, time;
	[time, msg].postln;
	if(g[\gen][2].isNil, {}, {
		g[\gen][2].free;
	});
	g[\gen].put(2, Synth(\trig, [\freq, 10]));
},'/trig_sync'));

)