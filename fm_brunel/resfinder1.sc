// SourceSeparation


// Amplitude, AverageOutput, FFTPower, FFTSubbandpower

(
{
	var in;
	in=SoundIn.ar;
	// a = AverageOutput.kr(in, Impulse.kr(1));
	a = Amplitude.kr(in);
	a.poll;
}.play
)
a.free;

(
{

var in, fft, entropy;

//in = SinOsc.ar(MouseX.kr(100,1000),0,0.1);
//in = Mix(SinOsc.ar([440,MouseX.kr(440,880)],0,0.1));
in= SoundIn.ar;

fft = FFT(LocalBuf(2048), in);

entropy=SpectralEntropy.kr(fft,2048,1);    //one output band (so full spectrum's entropy)

entropy.poll;

Out.ar(0,Pan2.ar(0.1*Blip.ar(100,10*(entropy.sqrt))));
}.play
)

// init once
(
// measurement
b = Bus.control(s, numChannels: 1);
// fft
c = Buffer.alloc(s, 2**12, 1);
// trigger
d = Bus.control(s, numChannels: 1);
// params
e = Bus.control(s, numChannels: 1);
)

// loop and do random search on parameter
// paramset 1: freq
(
SynthDef(\sin_freq, {|freq=400|
	var carrier;
	// carrier = SinOsc.ar(freq: freq, phase: 0, mul: 1, add: 0);
	// carrier = SinOsc.ar(freq: TRand.kr(100, 200, Impulse.kr(1)), phase: 0, mul: 3.0, add: 0);
	carrier = SinOsc.ar(freq: e.kr, phase: 0, mul: Line.kr(0.0, 1.0, dur: 1.0), add: 0);
	Out.ar(0, carrier);
}).add;

SynthDef(\meas_amp, {
	var in, chain;
	in = SoundIn.ar;
	// a = AverageOutput.kr(in, Impulse.kr(1));
	// a = Median.kr(length: 31, in: Amplitude.kr(in));
	chain = FFT(c.bufnum, in);
	a = FFTPower.kr(chain);
	// a = SpectralEntropy.kr(chain,2**12,1);
	// a.poll;
	Out.kr(b, a);
}).add;

// have sin_freq be controlled by searcher
// searcher 1: remember best and sample from there (gaussian vs. pareto)
// searcher 2: forward model search

// searcher 1
SynthDef(\searcher1, {|f0 = 400, m0 = 0|
	var trig_freq = 1.0;
	var trig_del = 0.5.reciprocal * 0.5;
	var trig, trig_d, trig_l;
	var nu, meas_ = -1.0, meas1, meas1_max, meas2, meas3, meas3_max, fout_, fout__, fout___, fout;
	var meas_trig, meas_plus;
	trig = Trig.kr(Impulse.kr(trig_freq));
	trig_d = TDelay.kr(trig, dur: trig_del);
	trig_l = Trig.kr(Impulse.kr(10));
	// FIXME: use exponential frewq space for linear perception
	nu = TGaussRand.kr(-1, 1, trig, mul: 5);
	// fout = TExpRand.kr(100, 4000, trig);
	// nu = XLine.kr(1, 4000, dur: 10);
	// nu.poll(1.0, \nu);
	fout__ = IRand(420, 550);
	fout_ = LocalIn.kr(1, fout__);

	SendTrig.kr(trig_l, 0, nu);
	// 1 send freq f_start + nu
	SendTrig.kr(trig_l, 9, fout_);
	fout = fout_ + nu;
	SendTrig.kr(trig_l, 1, fout);
	Out.kr(e, fout);
	// 2 measure freq
	meas1 = TrigAvg.kr(b.kr, trig_d);
	// meas = MeanTriggered.kr(b.kr, trig_d);
	meas1_max = RunningMax.kr(meas1, 0);
	meas2 = RunningSum.kr(b.kr, numsamp: 100) * 100.reciprocal;
	meas3 = RunningSum.kr(b.kr, numsamp: 20) * 20.reciprocal;
	meas3_max = RunningMax.kr(meas3, 0);
	SendTrig.kr(trig_l, 2, meas1);
	SendTrig.kr(trig_l, 3, meas1_max);
	SendTrig.kr(trig_l, 4, meas2);
	SendTrig.kr(trig_l, 5, meas3);
	// meas2.poll(10.0, label: \meas2);
	// meas_.poll(10.0, label: \meas_);
/*	if(meas_ < 0.0, {
		meas_ = meas2;
	});*/
	meas_plus = meas3 > meas3_max;
	SendTrig.kr(trig_l, 6, meas_plus);
	// meas_plus.poll(10.0, label: "meas3 > meas2");
	// TrigAvg, TDelay, MeanTriggered, MedianTriggered
	// 3 if meas2 > m_start, set f_start <- f_start + nu
	// meas_trig = Trig.kr(meas3 > meas2, dur: 0.01);
	// meas_trig = Trig.kr(meas_plus, dur: 0.1);
	meas_trig = Changed.kr(meas3_max);
	fout___ = fout__ + Latch.kr(DelayN.kr(nu, maxdelaytime: 1.0, delaytime: trig_del), meas_trig);
	LocalOut.kr(fout___);
	meas_ = Latch.kr(meas3_max, meas_trig);
	SendTrig.kr(trig_l, 7, fout_);
	SendTrig.kr(trig_l, 8, meas_);
	// fout_.poll(label: \fout_);
	// meas_.poll(label: \meas_);
	// meas_trig.poll(label: \meas_trig);
	// Out.kr(d, trig);
	// Out.kr(e, f0);
}).add;
)

// persistent shell that can be sent filenames, code for evaluation

// 1 record sweep: time, f, meas
// 2 room simulator (system)
// 3 lang based searcher
// 4 synth based searcher
// 5 repl
// 6 variation oscillator, measures, motivation

// - model fitting and prediction / searcher: gradient, cma-es, hyperopt
// - use external sound stimuli to activate the thing
// - energy: use energy

(
l = List.new();
// register to receive this message
o = OSCFunc({ arg msg, time;
    [time, msg].postln;
	l.add([time, msg]);
},'/tr', s.addr);


z = Synth(\sin_freq, [\freq, 440]);
y = Synth(\meas_amp, []);
x = Synth(\searcher1, []);
)

(
// l.size.postln;
// l[0][1][3].postln;
f = File("./l8.txt".standardizePath, "w");
// header
f.write("timestamp,oscaddr,nodeid,trigid,value\n");
// loop
l.do({|l_i|
	// l_i is a 2-elem list with timestamp, [data array]
	f.write(l_i[0].asCompileString);
	//f.putChar(Char.comma);
	l_i[1].do({|l_i_1|
		f.putChar(Char.comma);
		f.write(l_i_1.asCompileString);
	});
	f.putChar(Char.nl);
});
f.close();
o.free;
)

(
var freq_;
z = Synth(\sin_freq2, [\freq, 300]);
y = Synth(\meas_amp, []);

t = Task({
	inf.do({
		freq_ = 100.exprand(4000);
		["freq", freq_].postln;
		z.set(\freq, freq_);
		1.0.wait;
	});
}).play;



)