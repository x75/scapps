

s.boot;

//////////////////////////////
// busses
~abusses = [];
10.do({|i| ~abusses = ~abusses.add(Bus.audio());});

~cbusses = [];
10.do({|i| ~cbusses = ~cbusses.add(Bus.control());});

s.options.numOutputBusChannels;
s.options.numInputBusChannels;

// osc setup
NetAddr.broadcastFlag = true
~oscbroadcast = NetAddr("192.168.0.255", 1138);

~oscbroadcast.sendMsg("/blub", 100.rand);
~oscname = "theta";

// synth definitions
(
// main output mixer
~makeSynthDef = {
	arg name, busnum = 10;
	SynthDef(name, {
		arg out = 0, amp = 1.0;
		var busmix;
		busmix = Mix.fill(busnum,
			{|i| Pan2.ar(In.ar(4+i), -1 + (i*(2/busnum)))})
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
	var s1 = SinOsc.ar(Lag.kr(cellinval + LFNoise2.kr(10, mul: 20, add: 0), 0.1));
	var f1 = Pitch.kr(s1);
	// var f1 = ZeroCrossing.ar(s1);
	// var f1 = WhiteNoise.kr();
	// Poll.kr(Impulse.kr(2.0), f1, label: \u ++ NodeID.ir.poll);
	Out.kr(cellout, f1);
	Out.ar(out, s1);
	// SinOsc.ar({134.0.rand2(137.0)} ! 4, Lag.kr(Pitch.kr(~out.ar(4).reverse) * 0.1 * LFNoise2.kr(0.5, 4pi * 0.9), 0.1), 0.3) }
}).send(s);
)

// create mixer synth
~fmmixer = Synth(\fmmixer, [ \out, 0]);

// init control busses
(
~cbusses.do({|b, i|
	// b.value = 100.0 + (i * 100); // 100.rrand(400.0);
	b.value = 100.rrand(400.0);
});

~bus2osctask = Task({
	inf.do({|i|
		~cbusses.do({|b, j|
			~oscbroadcast.sendMsg("/" ++ ~oscname ++ "/c" ++ j, b.getSynchronous);
		});
		0.1.wait;
	});
});

)

// test scenario, create generators synths
(
~nodeids = [];
~abusses.do({|b, i|
	// Synth(\fm1, [\in, 0, \out, b, \amp, 1.0, \cellin, ~cbusses.wrapAt(i-1), \cellout, ~cbusses[i]]);
	~nodeids = ~nodeids.add(s.nextNodeID);
	["nodeid", ~nodeids[i]].postln;
	s.sendMsg("/s_new", "fm1", ~nodeids[i], 0, 1, "in", 0, "out", b.index, "amp", 1.0,
		"cellin", ~cbusses.wrapAt(i-1).index, "cellout", ~cbusses[i].index);
});

~bus2osctask.start;

)

~nodeids
~bus2osctask.stop;

~blub = ~cbusses[0].getSynchronous

~blub.value

// workspace ..
// osc to in bus
b = Bus.control(s);
b.value = 100.rrand(400.0);

c = Bus.control(s);
// out bus to osc

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