
(
// basic init
s.boot;
thisProcess.openUDPPort(1138); // open another custom listening port
thisProcess.openPorts;

// osc setup
NetAddr.broadcastFlag = true;
~oscbroadcast = NetAddr("192.168.0.255", 1138);

~oscbroadcast.sendMsg("/blub", 100.rand);
~oscnamelocal = "theta";
~oscnameremote = ["vrt", "hiaz"];
)

(
//////////////////////////////
// busses
~numabus = 10;
~numcbus = 10;

// local audio busses
~abusses = [];
~numabus.do({|i| ~abusses = ~abusses.add(Bus.audio());});

// local control busses
~cbusses = [];
~numcbus.do({|i| ~cbusses = ~cbusses.add(Bus.control());});

// remote control busses
// ~cbussesr = Dictionary.new;
~cbussesr = [];
(2*~numcbus).do({|i| ~cbussesr = ~cbussesr.add(Bus.control());});

s.options.numOutputBusChannels;
s.options.numInputBusChannels;

)

// synth definitions
(
this.executeFile("../src/supercollider/scapps/fm_counterflows/fmsynthdefs.sc");
)

(
// create mixer synth
~fmmixer = Synth(\fmmixer, [ \out, 0]);

// init local control busses
~cbusses.do({|b, i|
	// b.value = 100.0 + (i * 100); // 100.rrand(400.0);
	b.value = 100.rrand(400.0);
});

// init local control bus 2 osc
~bus2osctask = Task({
	inf.do({|i|
		~cbusses.do({|b, j|
			~oscbroadcast.sendMsg("/" ++ ~oscnamelocal ++ "/c" ++ j, b.getSynchronous);
		});
		0.1.wait;
	});
});

// init remote control bus from osc
(2*~numcbus).do({|i|
	OSCdef(\cr ++ i, {|msg, time, addr, recvPort|
		msg.postln;
		~cbussesr[i].value = msg[1];
	}, "/vrt/c" ++ i, recvPort: 1138);
});
)

// application specific code -> your ressonsibility

// test scenario, create generators synths
(
~nodeids = [];
~abusses .do({|b, i|
	// Synth(\fm1, [\in, 0, \out, b, \amp, 1.0, \cellin, ~cbusses.wrapAt(i-1), \cellout, ~cbusses[i]]);
	~nodeids = ~nodeids.add(s.nextNodeID);
	["nodeid", ~nodeids[i]].postln;
	s.sendMsg("/s_new", "fm1", ~nodeids[i], 0, 1, "in", 0, "out", b.index, "amp", 1.0,
		"cellin", ~cbussesr.wrapAt(i-1).index, "cellout", ~cbusses[i].index);
});

~bus2osctask.start;
)

p = Pipe.new("pwd", "r");            // list directory contents in long format
l = p.getLine;

~nodeids
~bus2osctask.stop;

~nodeidsdict = ();
~nodeidsdict.dump

~abusses

// workspace ..
// osc to in bus
~blub = ~cbusses[0].getSynchronous

~blub.value

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