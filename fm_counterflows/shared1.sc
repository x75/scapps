

s.boot;


p = ProxySpace.push(s);

~out = nil;
~out.play;

// main output mixer
p.clear
p = nil

~abusses = [];
10.do({|i| ~abusses = ~abusses.add(Bus.audio());});

~cbusses = [];
10.do({|i| ~cbusses = ~cbusses.add(Bus.control());});

s.options.numOutputBusChannels
s.options.numInputBusChannels

{|i| i} ! 10

10.do({|i| i.postln});

10.asInteger

(
~makeSynthDef = {
	arg name, busnum = 10;
	SynthDef(name, {
		arg out = 0, amp = 1.0;
		var busmix;
		busmix = Mix.fill(busnum,
			{|i|Pan2.ar(In.ar(4+i), -1 + (i*(2/busnum)))})
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
)

Synth(\fmmixer, [ \out,0]);


~cbusses.do({|b, i|
	b.value = 100.0 + (i * 100); // 100.rrand(400.0);
});

(
~abusses.do({|b, i|
	Synth(\fm1, [\in, 0, \out, b, \amp, 1.0, \cellin, ~cbusses[i], \cellout, c])
});
)

(

SynthDef(\fm1, {
	|in = 0, out = 0, amp = 1.0, cellin = 0, cellout = 0|
	var cellinval = In.kr(cellin);
	var s1 = SinOsc.ar(cellinval, 1.2);
	// var f1 = Pitch.kr(s1);
	var f1 = ZeroCrossing.ar(s1);
	// var f1 = WhiteNoise.kr();
	Poll.kr(Impulse.kr(2.0), f1);
	Out.kr(cellout, f1 * 1e-3);
	Out.ar(out, s1);
	// SinOsc.ar({134.0.rand2(137.0)} ! 4, Lag.kr(Pitch.kr(~out.ar(4).reverse) * 0.1 * LFNoise2.kr(0.5, 4pi * 0.9), 0.1), 0.3) }
}).send(s);
)

// osc to in bus
b = Bus.control(s);
b.value = 100.rrand(400.0);

c = Bus.control(s);
// out bus to osc
i = s.nextNodeID;
s.sendMsg("/s_new", "fm1", i, 0, 1, "in", 0, "out", 0, "amp", 1.0, "cellin", b.index, "cellout", c.index);

s.sendMsg("/n_free", i);
s.sendMsg("/s_get", i, "in");

// audio in/out busses

x = Synth.new(\fm1, [\cellin, b, \cellout, c]);
x.free

x = Synth.basicNew(\fm1, s, i); //[\cellin, b, \cellout, c]);
x.newMsg()

s.sendBundle(nil, x.newMsg;);
s.queryAllNodes;

s.dumpOSC()

b.get()
b.value

c.get
c.index
c.rate
c.scope

b.scope