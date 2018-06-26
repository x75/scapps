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
c = Buffer.alloc(s, 2**14, 1);
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
	carrier = SinOsc.ar(freq: e.kr, phase: 0, mul: 3.0, add: 0);
	Out.ar(0, carrier);
}).add;

SynthDef(\meas_amp, {
	var in, chain;
	in = SoundIn.ar;
	// a = AverageOutput.kr(in, Impulse.kr(1));
	// a = Median.kr(length: 31, in: Amplitude.kr(in));
	chain = FFT(c.bufnum, in);
	a = FFTPower.kr(chain);
	a.poll;
	Out.kr(b, a);
}).add;

// have sin_freq be controlled by searcher
// searcher 1: remember best and sample from there (gaussian vs. pareto)
// searcher 2: forward model search

// searcher 1
SynthDef(\searcher1, {|f0 = 100, m0 = 0|
	var trig, trig_d, nu, meas_ = 0.0, meas, fout_ = 100, fout;
	var meas_trig;
	trig = Trig.kr(Impulse.kr(2));
	trig_d = TDelay.kr(trig, dur: 0.05);
	nu = TGaussRand.kr(-1, 1, trig, mul: 100);
	nu.poll;
	// 1 send freq f_start + nu
	fout = fout_ + nu;
	Out.kr(e, fout);
	// 2 measure freq
	// meas = TrigAvg.kr(b.kr, trig_d);
	// meas = MeanTriggered.kr(b.kr, trig_d);
	meas = RunningMax.kr(b.kr, 0);
	meas.poll;
	// TrigAvg, TDelay, MeanTriggered, MedianTriggered
	// 3 if meas > m_start, set f_start <- f_start + nu
	// meas_trig = Trig.kr(meas > meas_);
	meas_trig = Changed.kr(meas);
	fout_ = Latch.kr(fout, meas_trig);
	meas_ = Latch.kr(meas, meas_trig);
	fout_.poll;
	meas_.poll;
	meas_trig.poll;
	// Out.kr(d, trig);
	// Out.kr(e, f0);
}).add;

)


(
var bla;
z = Synth(\sin_freq, [\freq, 440]);
y = Synth(\meas_amp, []);
x = Synth(\searcher1, []);
)