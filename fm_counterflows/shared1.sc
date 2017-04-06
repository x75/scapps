
(
// load configuration from shared_local.sc first
// copy shared_template.sc to shared_local.sc and adjust your settings

~counterpath=("../src/supercollider/scapps/fm_counterflows/");
this.executeFile(~counterpath +/+ "shared_local.sc");
)

(
// basic init for server and network
s.boot;
thisProcess.openUDPPort(1138); // open another custom listening port
thisProcess.openPorts;

// network setup / osc foo
NetAddr.broadcastFlag = true;
~oscbroadcast = NetAddr("192.168.0.255", 1138);
)

(
//////////////////////////////
// local audio busses
~abusses = [];
~numabus.do({|i| ~abusses = ~abusses.add(Bus.audio());});

// remote control busses
~cbussesr = Dictionary.new;
~oscnameremote.keys.do({|name, i|
	var tmp = [];
	(~oscnameremote[name]).do({|i|
		tmp = tmp.add(Bus.control());
		// init bus values
		// if(name == ~oscnamelocal, {
		tmp[tmp.size-1].value = -1.0.rrand(1.0);
		//});
	});
	~cbussesr.add(name -> tmp);
});

// s.options.numOutputBusChannels;
// s.options.numInputBusChannels;
)

// load synth definitions from separate files
(
// shared synths (mixer, reference)
this.executeFile(~counterpath +/+ "fmsynthdefs.sc");
// local definitions
this.executeFile(~counterpath +/+ "fmsynthdefs_" ++ ~oscnamelocal ++ ".sc");
)

(
// create mixer synth
~fmmixer = Synth(\fmmixer, [ \out, 0, \busoffset, ~abusses[0].index]);

// init local control bus 2 osc
~bus2osctask = Task({
	inf.do({|i|
		~cbussesr[~oscnamelocals].do({|b, j|
			~oscbroadcast.sendMsg("/" ++ ~oscnamelocal ++ "/c" ++ j, b.getSynchronous);
		});
		0.1.wait;
	});
});

// ~bus2osctask.play;
// OSCdef.all.size;
// OSCdef.freeAll;

// init remote control bus from osc
~oscnameremote.keys.do({|name, i|
	(~oscnameremote[name]).do({|j|
		OSCdef((name ++ "cr" ++ j).asSymbol, {|msg, time, addr, recvPort|
			//msg.postln;
			// linear assignment
			~cbussesr[name][j].value = msg[1];
			// random assignment
			// ~cbussesr[~cbussesr.keys.choose][~numcbus.rand].value = msg[1];
		}, "/" ++ name ++ "/c" ++ j, recvPort: 1138);
	});
});
)

// application specific code -> your turn

// test scenario, create generators synths
(
~nodeids = [];
~abusses.do({|b, i|
	// Synth(\fm1, [\in, 0, \out, b, \amp, 1.0, \cellin, ~cbusses.wrapAt(i-1), \cellout, ~cbusses[i]]);
	~nodeids = ~nodeids.add(s.nextNodeID);
	["nodeid", ~nodeids[i]].postln;
	s.sendMsg("/s_new", ~synthdefs.choose, ~nodeids[i], 0, 1, "in", 0, "out", b.index, "amp", 1.0,
	"cellin", ~cbussesr[~cbussesr.keys.choose].wrapAt(~numcbus.rand).index, "cellout", ~cbussesr[~oscnamelocals][i].index);
	// local only hack
	// "cellin", ~cbusses.wrapAt(~numcbus.rand).index, "cellout", ~cbusses[i].index);
});

~bus2osctask.start;
)

(
~nodeids.do({|id, i|
	s.sendMsg("/n_free", id);
});
~bus2osctask.stop;
)

///////////////////////////////////////////////////////////////////////////////////////////////
// workspace ..


// get current working directory of sclang process
p = Pipe.new("pwd", "r");            // list directory contents in long format
l = p.getLine;

p = nil
~nodeids

~nodeidsdict = ();
~nodeidsdict.dump

// ~oscbroadcast.sendMsg("/" ++ ~oscnamelocal ++ "/c" ++ j, b.getSynchronous);
~cbusses[2].value = 100.rrand(1000);
~cbusses[2].getSynchronous

~abusses

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


~testarray = {|i| i} ! 10;
~
