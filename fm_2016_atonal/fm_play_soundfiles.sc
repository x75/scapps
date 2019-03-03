// soundfile player



// f = SoundFile.new;
// f.openRead("/home/lib/audio/sounds/ambi/")
// f.numFrames;
// f.sampleRate;
// f.dump;



////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

(
~sflist = File.new("/home/src/supercollider/scapps/fm_atonal16/sounds_wasps.txt", "r");

~sflist.seek(0);
~sfs = ~sflist.readAllString.split($\n); //.postln;
~sfs.removeAt(~sfs.size-1);
[~sfs.size, ~sfs].postln;
~sflist.close;

~sfs_bufs = Array.new(~sfs.size);
~sfs_lens = Array.new(~sfs.size);

~sfs.do({|sf|
	var segdur;
	~sfs_bufs = ~sfs_bufs.add(Buffer.read(s, sf));
	f = SoundFile.new;
	f.openRead(sf);
	//f.numFrames.postln;
	//f.numChannels.postln;
	// l = (1000*f.numFrames/f.sampleRate).ceil/1000;
	segdur = f.numFrames/f.sampleRate;
	// [tpsb, segdur, segdur/tpsb].postln;
	// l = (segdur/tpsb).round;
	// l = l / 4;
	// l = 4;
	// ((60000/bpm)/4)*13
	// ["l", l].postln;
	~sfs_lens = ~sfs_lens.add(segdur);
});
)

(
SynthDef(\help_PlayBuf, {| out = 0, bufnum = 0, amp = 0.1, dur = 0.1
	pitch = 1.0|
	var aenv, bufsig;
	dur = dur;
	aenv = Env([0, 1, 1, 0], [0.001, dur - 0.002, 0.001]);
	//
	bufsig = PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum) * pitch, doneAction:2) * EnvGen.kr(aenv, doneAction: 2) * amp;
    Out.ar(out, Pan2.ar(bufsig, SinOsc.kr(0.1, 0)))
}).add;
)

p = Synth(\help_PlayBuf, [\out, 0, \bufnum, ~sfs_bufs.choose, \amp, 0.5]);

// ~t1_wasps_dur = 0.1;
~t1_wasps_dur = Prand([0.1, 0.2, 0.3, 0.4, 1.0, 2.0, 0.05, 0.05], inf).asStream; 
~t1_wasps_pit = Prand((0.125 ! 4) ++ (0.25 ! 4) ++ (0.5 ! 4) ++ (1.0 ! 4) ++ [2.0], inf).asStream; 

~t1_wasps_dur = Pgauss(0.1, 0.01, inf).asStream;
~t1_wasps_pit = Pgauss(Pfunc({|i| (sin(i / 100.0 * 2 * pi) / 2.0 + 0.5) * 0.01 + 0.25}), 0.01, inf).asStream;

~t1_wasps_pit = Pgauss(Pfunc({|i| (sin(i / 100.0 * 2 * pi) / 2.0 + 0.5) * 0.01 + 0.125}), 0.01, inf).asStream;

(
~t1_wasps = Task({
	var dur;
	var pit;
	inf.do({|i|
		dur = ~t1_wasps_dur.next;
		pit = ~t1_wasps_pit.next(i);
		b = ~sfs_bufs.choose;
		["playing buffer", b, "dur", dur, "pit", pit].postln;
		p = Synth(\help_PlayBuf, [\out, 0, \bufnum, ~sfs_bufs.choose, \amp, 0.5,
			\pitch, pit, \dur, dur * 0.6]);
		q = Synth(\help_PlayBuf, [\out, 0, \bufnum, ~sfs_bufs.choose, \amp, 0.5,
			\pitch, pit + rand(-0.1, 0.1), \dur, dur * 0.6]);
		dur.wait;
			
	});
});
~t1_wasps.start;
)
~t1_wasps

~t1_wasps.stop;




////////////////////////////////////////////////////////////////////////////////
// test loading maximum number of buffers
a = Array.new(1024);
1000.do({|i|
	["loading", i].postln;
	a = a.add(Buffer.read(s, "/home/lib/audio/sounds/ambi/wasps_wall_STE-011_noise_removed_cut/wasps_wall_STE-011_noise_removed_229.196.wav"));
	[a[i].bufnum].postln;
});

a.do({|buf|
	buf.free;
});

s.freeAll
