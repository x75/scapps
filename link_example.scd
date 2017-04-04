
s.boot;

Link.enable;

(
SynthDef("trig1",{ arg freq1 = 800, freq2 = 1200, dur=0.125, tdiv = 4, amp = 0.5;
	    var gate;
	    gate = LinkTrig.kr(tdiv);
	    Out.ar(0,
		        SinOsc.ar([freq1, freq2] * LFSaw.kr(0.1, iphase: 0, mul: 4, add: 1), 0.3) * EnvGen.kr(
			            Env([0,0.1,0.1,0],[0.01,1.0,0.01],[-4,4],2),
			            gate,
			timeScale: 0.5,
			levelScale: amp,
			            doneAction: 0)
    )
}).send(s);
)

x = Synth.new(\trig1, [\freq1, 200.rand2(700), \freq2, 200.rand2(700)]);

x.set(\tdiv, 2);
