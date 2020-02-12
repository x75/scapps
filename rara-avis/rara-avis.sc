////////////////////////////////////////////////////////////
// rara avis, contribution by
// oswald berthold 2008, 2009

// version 5
// bXXX: doppler-effect: f = f_0(v/v+v_r)
// bXXX: doerner
// bXXX: todt / blackbird
// bXXX: bird / physmod
// bXXX: replace java with scum, scgraph or fluxus, or pygl

// to start it

( // eval block 1

Server.default = s;
s.waitForBoot {
	~asbus = Bus.audio(s, 3);
	~asbus.index;
	~noisebus = Bus.audio(s, 1);
};
//SwingOSC.java;
//SwingOSC.java = "/usr/lib/jvm/java-1.5.0-sun-1.5.0.16/bin/java";
// ~g1 = SwingOSC.default;
// ~g1.boot;
)

( // eval block 2

// load synthdefs
this.executeFile("/home/lib/code/supercollider/rara-avis-synths.sc");
)

( // eval block 3

~source_mtx_init = {|num_src_type, width, height, source_prob|
	Array.fill3D(num_src_type, width, height, {|x,y,z|
		if(source_prob.coin, {x}, {-1});
	});
};

~source_mtx_update = {|mtx, ix, iy, iz|
	var x, y, z, i1, i2;
	x = mtx.size;
	y = mtx[0].size;
	z = mtx[0][0].size;
	//["pre src mtx update", ix, iy, iz].postln;
	mtx[ix][iy][iz] = -1;
	//["reset matrix"].postln;
	while({
		i1 = y.rand;
		i2 = z.rand;
		//["new index and val ol", i1, i2, mtx[ix][i1][i2]].postln;
		mtx[ix][i1][i2] != 1.neg}, {
			//["new index and val il", i1, i2, mtx[ix][i1][i2], (mtx[ix][i1][i2] != 1.neg)].postln;
			//i1 = y.rand;
			//i2 = z.rand;
		});
	//["src mtx found index", ix, i1, i2].postln;
	mtx[ix][i1][i2] = ix;
	//["post src mtx update"].postln;
	//^mtx
};
// draw source matrix for debugging
~draw_source_mtx = {|mtx|
	var win, sx, sy, sz;
	//~source_mtx_win
	sx = mtx.size;
	sy = mtx[0].size;
	sz = mtx[0][0].size;
	"draw_source_mtx".postln;
	win = Window("sources", Rect( 10, 10, sy*~unit, sz*~unit), true).front;
	//~source_mtx_win.
	win.view.background_( Color.white );
	// ~source_mtx_win.
	win.drawHook = {
		Pen.scale(~unit, ~unit);
		sx.do{|x|
			Pen.color = ~source_colours[x];
			sy.do {|y|
				sz.do {|z|
					//["raw_source_mtx", x,y,z].postln;
					//JPen.strokeRect(Rect(x*~unit, y*~unit, ~unit, ~unit));
					if(mtx[x][y][z] >= 0, {
						Pen.fillRect(Rect(y, z, 1, 1)); //.translate();
						Pen.stroke;
					});
				};
			};
		};
	};
};
)
//~source_mtx_update.value(~source_mtx, 0, 11, 27);

( // eval block 4

//o = Synth(\asdec, [\inchan, ~asbus.index]);
// bird dict and brain-synth communication
// number of cells on each dimension
~width = 22;
~height = ~width;
~unit = 6; // unit = l = width/height, 
~num_src_type = 4;
// do graphics
~do_gfx = false;
// birds storage
~birds_alive = Dictionary.new;
// maximum allowed number of birds
~birds_max = 14;
// velocity of movement
~birds_vel = 1.0;
// setup source matrix
// source mean density
~source_prob = 0.006125; //0.0125; //0.02520.squared.reciprocal;
//~source_prob = 0.0125; //0.02520.squared.reciprocal;
// XXX: sources should have extension in space: center and surrounding
// XXX: 3D sourre mtx: 2D is space, 3rd D is source type
//~source_mtx = Array.fill2D(~width, ~height, {if(~source_prob.coin, {(1..4).choose}, {0})});
~source_mtx = ~source_mtx_init.value(~num_src_type, ~width, ~height, ~source_prob);
// source colours
~source_colours = [Color.red, Color.green, Color.blue, Color.yellow];
if(~do_gfx, {~draw_source_mtx.value(~source_mtx)});
)

( // eval block 5

// brain update, strategy #1
~eval_strategy_1 = {|key, type, bx, by, wh, hh|
	var dir;
	//["check for source hit"].postln;
	if(~source_mtx[type][bx+wh][by+hh] >= 0, {
		["strat 1: source hit"].postln;
		// set velocity to 0
		~birds_alive[key][5] = 0;
		// refill store
		if(~birds_alive[key][4][type] < 1, {
			~birds_alive.at(key)[4][type] = (~birds_alive[key][4][type] + 0.1).clip(0.0, 1.01); // XXX: value-strangeness
			// 				if(~birds_alive.at(key)[4][i] > 1, {~birds_alive.at(key)[4][i] = 1.01}); // limit store level
		}, {
			["src hit + refilled", type, bx, by].postln;
			dir = (2 * pi).rand;
			~birds_alive.at(key)[3] = dir; // set new random direction
			~birds_alive.at(key)[5] = ~birds_vel; // reset velocity
			// source update: disappear + reappear
			~source_mtx_update.value(~source_mtx, type, bx+wh, by+hh);
			//~birds_alive.at(key)[0] = bx + cos(dir) - 30;
			//~birds_alive.at(key)[1] = by + sin(dir) - 30;
		});
	});
};

// angular search
~eval_strategy_2_src_radar = {|key, type, bx, by, wh, hh|
	// XXX: see [[mathe#problems]], radius/angularily quantized circle
	// XXX: need to visit every cell in space anyway and ask wether occupied by source or not
	//      so we don't need angular search and can do plain iteration
	var dir, idx_y, idx_z, center;
	var circ_seg, tnum, rslt;
	var win, cols, width, w2;
	var mtx = Array.fill3D(1, ~width, ~width, -1); // always square? n.n.
	// generate w(beta) = f(x_beta)
	// XXX: given an angle, how to find out nearest object
	// XXX: circular search
	//orx = bx.floor; ory = by.floor;
	center = Complex(bx.floor, by.floor).asPolar;
	cols = [Color.black, Color.red, Color.green, Color.yellow];
	width = ~width; //59; ///0.92;
	w2 = (width/2).trunc-1;
	//win = GUI.window.new("", Rect(0, 0, width*~unit, width*~unit), true);
	//win.view.background_( Color.white );
	//win.front;
	//win.drawHook = {
		//GUI.pen.moveTo(Point(0, 0));
		//GUI.pen.lineTo(Point(2,2));
		//GUI.pen.stroke;
		w2.do {|i| // four circles
			//GUI.pen.color = Color.new(i/(w2*3), 0, 0, 1); // Color.rand(0.0, 0.3); //cols[i];
			// XXX: see problem at beginning of function
			circ_seg = (i + 1 * 12);
			circ_seg.do {|j|
				//[i,j].postln;
				//tnum = (center.asPolar + Polar(((i+1.0)*0.92), (j*2*pi)/circ_seg)).asComplex;
				tnum = (center.asPolar + Polar(i+1.0, (j*2*pi)/circ_seg)).asComplex;
				//[i, j, tnum.real+wh, tnum.imag+hh].postln;
				//rslt = ~source_mtx[type][tnum.real+wh%~width][tnum.imag+hh%~width];
				idx_y = tnum.real+wh%~width;
				idx_z = tnum.imag+hh%~width;
				rslt = ~source_mtx[type][idx_y][idx_z];
				mtx[0][idx_y][idx_z] = rslt;
				if(rslt>=0, {
					//GUI.pen.color = ~source_colours[type];
					//GUI.pen.fillRect(Rect(tnum.real.ceil+w2%~width*~unit, tnum.imag.ceil+w2%~width*~unit, ~unit, ~unit));
				});
				//[i, j, tnum.round, rslt].postln;
			};
		};
	//}; // drawHook
	mtx
};
//~tmp = ~eval_strategy_2_src_radar.value(1006, 0, 10, 0, 30, 30);

~eval_strategy_2_weight = {|mtx, a, bx, by|
	var rslt, e, ef;
	//a.postln;
	//["eval_s2_weight", bx, by].postln;
	block {|break|
		a.do {|e|
			ef = e + (~width/2) + [bx, by] % ~width;
			rslt = mtx[0][ef[0]][ef[1]];
			if(rslt >= 0, {
				//[e, Complex(e[0], e[1]).asPolar, rslt].postln;
				break.value(e);
			});
		};
		nil
	};
};

~eval_strategy_2_indices = {|mtx, bx, by|
	var sx, sy, sz;
	var w2, indices, rslt;
	sx = mtx.size;
	sy = mtx[0].size;
	sz = mtx[0][0].size;
	w2 = sy/2;
	//indices = Array.fill(8*Array.series(w2, 1, 1).sum, [0, 0]);
	//indices.size.postln;
	block {|break|
		(1..(w2-1)).do {|i| // assume mtx square
			//i.postln;
			indices = [[i, 0], [0, i], [i.neg, 0], [0, i.neg]];
			//indices.postln;
			rslt = ~eval_strategy_2_weight.value(mtx, indices, bx, by);
			//["l1", rslt].postln;
			if(rslt != nil, {break.value(rslt)});
			(i-1).do {|j|
				j = j+1;
				indices = [[i, j], [j, i], [j.neg, i], [i.neg, j], [i.neg, j.neg], [j.neg, i.neg], [j, i.neg], [i, j.neg]]; // uff
				//indices.postln;
				rslt = ~eval_strategy_2_weight.value(mtx, indices, bx, by);
				if(rslt != nil, {break.value(rslt)});
			};
			indices = [[i, i],
				[i.neg, i],
				[i.neg, i.neg],
				[i, i.neg]];
			//indices.postln;
			rslt = ~eval_strategy_2_weight.value(mtx, indices, bx, by);
			if(rslt!=nil, {break.value(rslt)});
		};
		//nil
	};
};
// // testing
// ~rslt = Array.fill(~num_src_type, {[0, 0]});
// ~num_src_type.do {|type|
// 	~tmp = ~eval_strategy_2_src_radar.value(1006, 0, 10, 0, 30, 30);
// 	~hit = ~eval_strategy_2_indices.value(~tmp, 5, 2);
// 	~rslt[type] = Complex(~hit[0], ~hit[1]).asPolar;
// };
// ~rslt[~rslt.magnitude.minIndex].postln;
// // testing 2
// ~rslt = Array.fill(~num_src_type, {[0, 0]});
// ~num_src_type.do {|type|
// 	~tmp = Array.fill3D(1, ~width, ~height, {1.neg});
// 	~tmp[0] = ~source_mtx[type];
// 	~hit = ~eval_strategy_2_indices.value(~tmp, -2, 20);
// 	~rslt[type] = Complex(~hit[0], ~hit[1]).asPolar;
// };
// [~rslt.magnitude.minIndex, ~rslt[~rslt.magnitude.minIndex]].postln;

~eval_strategy_2 = {|key, type, bx, by, wh, hh|
	var dir;
	//["check for source hit, s2"].postln;
	bx = bx.round; // bx.abs.trunc * bx.sign;
	by = by.round; // by.abs.trunc * by.sign;
	if(~source_mtx[type][bx+wh][by+hh] >= 0, {
		["strat 2: source hit"].postln;
		// set velocity to 0
		~birds_alive[key][5] = 0;
		~birds_alive[key][0] = bx;
		~birds_alive[key][1] = by;
		// refill store
		if(~birds_alive[key][4][type] < 1, {
			~birds_alive.at(key)[4][type] = (~birds_alive[key][4][type] + 0.1).clip(0.0, 1.01); // XXX: value-strangeness
			// 				if(~birds_alive.at(key)[4][i] > 1, {~birds_alive.at(key)[4][i] = 1.01}); // limit store level
		}, {
			["src hit + refilled", type, bx, by].postln;
			// strat 2 kern
			~rslt = Array.fill(~num_src_type, {Complex(wh, hh).asPolar});
			~num_src_type.do {|type|
				//["eval s2", "scanning for neighbour", type].postln;
				~tmp = Array.fill3D(1, ~width, ~height, {1.neg});
				~tmp[0] = ~source_mtx[type];
				~hit = ~eval_strategy_2_indices.value(~tmp, bx, by);
				//["eval s2", "hit", ~hit].postln;
				if(~hit.size > 0, {
					~rslt[type] = Complex(~hit[0], ~hit[1]).asPolar;
				});
			};
			[~rslt.magnitude.minIndex, ~rslt[~rslt.magnitude.minIndex], ~rslt[~rslt.magnitude.minIndex].asComplex + Complex(bx, by)].postln;
			dir = ~rslt[~rslt.magnitude.minIndex].angle;
			//["post setting dir", dir].postln;
			//~hit = ~eval_strategy_2_indices.value(~tmp, bx, by);
			//dir = Complex(~hit[0], ~hit[1]).asPolar.angle;
			//dir = (2*pi).rand;
			~birds_alive.at(key)[3] = dir; // set new random direction
			~birds_alive.at(key)[5] = ~birds_vel; // reset velocity
			// source update: disappear + reappear
			//["ub pre src mtx update"].postln;
			~source_mtx_update.value(~source_mtx, type, bx+wh, by+hh);
			//["ub post src mtx update"].postln;
			//~birds_alive.at(key)[0] = bx + cos(dir) - 30;
			//~birds_alive.at(key)[1] = by + sin(dir) - 30;
		});
	});
};
//);
//                     id    tp x  y  w   h
// XXX: return the whole matrix, for all types
// XXX: 2009-02-11: alles mist so: einfach ueber matrix iterieren, fuer
//      jede koordinate betrag/winkel ausrechnen, fertig, in 2D array packen
// XXX: 2009-02-12: doch nicht so einfach, da die abstaende ungleich sind für
//      pixel in gleicher spalte aber untersch. zeile, back to angular method
//~source_mtx[0][29][29];
//~draw_source_mtx.value(~tmp);
//{{|i| i.tanh}!1000}.bench

//Complex(10.0.rand, 10.0.rand).trunc
//6.1230317691119e-17.trunc
~brain_update = {|key|
	// XXX: get wh/hh out of here
	// XXX: pack edge collision into func
	// XXX: pack death detection into func
	var bx, by, dir, brho, btheta, vel;
	var wh, hh;
	wh = ~width/2;
	hh = ~height/2;
	bx = (~birds_alive[key][0]); // + wh);
	by = (~birds_alive[key][1]); // + hh);
	dir = ~birds_alive[key][3]; // direction
	vel = ~birds_alive[key][5];
	//["brain update", bx, by, dir].postln;
	// catch edge collision
	// XXX: either reflect or reenter from opposite side and keep angle
	if((bx < (wh.neg - 0.5)), { // 0), {
		["boundary x links"].postln;
		bx = ~width + bx; // wh-1; // ;
		~birds_alive.at(key)[0] = bx; // - wh;
// 		dir = pi - dir;
// 		~birds_alive.at(key)[3] = dir;
	});
	if((by < (hh.neg - 0.5)), { // 0), {
 		["boundary y oben"].postln;
		by = ~height + by; // hh-1; // ;
		~birds_alive.at(key)[1] = by; // - hh;
// 		dir = dir.neg;
// 		~birds_alive.at(key)[3] = dir;
	});
	if((bx >= (wh-0.5)), { // (~width-1)), {
 		["boundary x rechts"].postln;
		bx = bx - ~width; // wh.neg; //  // 0;
		~birds_alive.at(key)[0] = bx; //wh.neg;
// 		dir = pi - dir;
// 		~birds_alive.at(key)[3] = dir;
	});
	if((by >= (hh-0.5)), { // (~height-1)), {
 		["boundary y unten"].postln;
		by = by - ~height; // hh.neg; //  // 0;
		~birds_alive.at(key)[1] = by; // hh.neg;
// 		dir = dir.neg;
// 		~birds_alive.at(key)[3] = dir;
	});
	// XXX: make dot stick around source for a while, recharge interval (deviation from model)
	//	["brain up", "pre eval strat", bx, by].postln;
	~num_src_type.do {|i|
		// check if entry still exists
		if(~birds_alive.at(key) != nil, {
			// decrease store of type i
			~birds_alive.at(key)[4][i] = ~birds_alive.at(key)[4][i] - 0.005; // XXX: vari-ablize
			//["bird", key, "store", i, ~birds_alive.at(key)[4][i]].postln;
			//~birds_alive.at(key)[2].red = 
			// check for store of type i
			if(~birds_alive[key][4][i] <= 0, {
				// remove bird synth and entry
				["bird dying of drained source type", i].postln;
				s.sendBundle(nil, ["/n_free", key]);
				// removing this might be a problem when the loop returns to this key
				~birds_alive.removeAt(key);
				"bird dead".postln;
				//^nil
				//~birds_alive.at(key)[4][i] = 0;
			}, {
				~eval_strategy_2.value(key, i, bx, by, wh, hh);
			});
		});
		// debug
		// brain update
		//~eval_strategy_1.value(key, i, bx, by, wh, hh);
	};
	if(~birds_alive.at(key) != nil, {
		//["strat 2: going on"].postln;
		dir = ~birds_alive[key][3];
		vel = ~birds_alive[key][5];
		// 	~birds_alive.at(key)[0] = bx + (vel * cos(dir)); // - wh;
		// 	~birds_alive.at(key)[1] = by + (vel * sin(dir)); // - hh;
		~birds_alive.at(key)[0] = ~birds_alive.at(key)[0] + (vel * cos(dir)); // - wh;
		~birds_alive.at(key)[1] = ~birds_alive.at(key)[1] + (vel * sin(dir)); // - hh;
	});
	//~birds_alive[key].postln;
};
)

( // eval block 6

// draw region with sources and birds
~drawbirds = {
	var wh, hh;
	wh = ~width/2;
	hh = ~height/2;
	"~drawbirds: pre win".postln;
	~win = Window("bird plane", Rect( 10, 0, ~width*~unit, ~height*~unit ), false).front;
	//~win.front;
	"~drawbirds: pre background".postln;
	~win.view.background_( Color.white );
	//~pnt = Point(10, 10);
	~mytriangle = [Point(0.0, 0.0), Point(1.0, 0.0), Point(cos(pi/3), sin(pi/3))];
	~mytriangle_up = [Point(0.0, 1.0), Point(1.0, 1.0), Point(cos(pi/3), 1-sin(pi/3))];
	"~drawbirds: pre addShape".postln;
	~addShape = {|p|
		//p.postln;
		//Pen.moveTo(p[0]);
		(p.size - 1).do {|i|
			Pen.line(p[i], p[i+1]);
		};
		//Pen.line(p[1], p[2]);
		Pen.line(p[p.size-1], p[0]);
	};
	// draw sources method
	~draw_mtx = [nil, {|x,y|
		~addShape.value(~mytriangle.scale(~unit).translate(Point(x*~unit, y*~unit)));
		Pen.stroke;
	}, {|x,y|
		~addShape.value(~mytriangle_up.scale(~unit).translate(Point(x*~unit, y*~unit)));
		Pen.stroke;
	}, {|x,y|
		Pen.strokeOval(Rect(x*~unit, y*~unit, ~unit, ~unit));
	}, {|x,y|
		Pen.strokeRect(Rect(x*~unit, y*~unit, ~unit, ~unit));
	}];
	~win.drawHook = {
		Pen.color = Color.black; //red( rrand( 0.0, 1 ), rrand( 0.0, 0.5 ));
		//Pen.moveTo(~pnt);
		//Pen.lineTo(Point(100,200));
		//Pen.lineTo(Point(rrand(100, 200),rrand(100, 200)));
		~num_src_type.do {|x|
			~width.do {|y|
				~height.do {|z|
					// add 1: -1 becomes nil, the rest becomes shapes
					~draw_mtx[~source_mtx[x][y][z]+1].value(y, z);
					//if(~source_mtx[x][y], {
					//.choose.value;
					//});
					// 				if(prob.coin, {
					// 				});
					// 				if(prob.coin, {
					// 				});
					// 				if(prob.coin, {
					// 				});
					//Pen.addArc(Point(400, 300), 0.5*~unit, 0, 2pi);
					//Pen.stroke;
					//Pen.fillRect(Rect(520, 410, 10, 10));
				};
			};
		};
		// moving dot
		//Pen.color = Color.red;
		//Pen.fillRect(Rect((~width*~unit).rand, (~height*~unit).rand, 2, 2));
		//Pen.stroke;
		~birds_alive.do {|ba|
			//ba.postln;
			Pen.color = ba[2];
			//Pen.fillRect(Rect(ba[0]*~width*~unit, ba[1]*~height*~unit, 10, 10));
			Pen.fillRect(Rect(ba[0] + wh * ~unit, ba[1] + hh * ~unit, ~unit, ~unit));
			Pen.stroke;
		};
	};
};
//~drawbirds.value();
//~win.refresh
// )
// (
// var run = true;
// ~win.onClose = { run = false }; // closing window stops animation
// { while { run } { ~win.refresh; 0.75.wait }}.fork( AppClock );
// )
// (

~add_bird = {|asbus, num_src_type, ind|
	var seglen, genhead, synthid, bx, by;
	seglen = 20;
	genhead = 10;
	synthid = s.nextNodeID; // 2000 + 0.rrand(100);
	bx = 30.0.rand2;
	by = 30.0.rand2;
	// keep books about the birds: 
	//                           x,  y,  color,               direction,   store-level (array),           velocity
	~birds_alive.add(synthid -> [bx, by, Color.red(val: 1.0), (2*pi).rand, Array.fill(num_src_type, 1.0), ~birds_vel]);
	// make bird synth
	s.sendBundle(nil,
		["/s_new", "fofsynth4", synthid, 0, 0, \out, asbus,
			\amp, 0.1, \dur, ind[1], \plvl, ind[2], \pbias,
			ind[3], \flvl, ind[4], \fbias, ind[5], \pnoiselvl, 30, \fnoiselvl,
			30, \callfreq, ind[6], \callduty, ind[7], \calldrift,
			ind[8], \callmodreq, ind[9], \az, Complex(bx, by).theta,
			\gn, (1-(Complex(bx, by).rho/930)).squared], // start bird
		["/n_setn", synthid, "penv1d", 20] ++ ind[((0*seglen+genhead)..(1*seglen+genhead-1))],
		["/n_setn", synthid, "penv2d", 20] ++ ind[((1*seglen+genhead)..(2*seglen+genhead-1))],
		["/n_setn", synthid, "fenv1d", 20] ++ ind[((2*seglen+genhead)..(3*seglen+genhead-1))],
		["/n_setn", synthid, "fenv2d", 20] ++ ind[((3*seglen+genhead)..(4*seglen+genhead-1))],
		["/n_setn", synthid, "fenv3d", 20] ++ ind[((4*seglen+genhead)..(5*seglen+genhead-1))],
		["/n_setn", synthid, "fenv4d", 20] ++ ind[((5*seglen+genhead)..(6*seglen+genhead-1))],
		["/n_setn", synthid, "aenv1d", 20] ++ ind[((6*seglen+genhead)..(7*seglen+genhead-1))]
	); // end bird synth
};
)

( // eval block 7 final

// notes <2009-02-13 Fri>
// hangs: hang in source_mtx_update, because the if doesnt catch: update sc version?: while
// missing sources: float indices? unklar.
// koordinaten / mtx index glattbuegeln: sieht alles gut aus <2009-02-14 Sat>
// in Klasse verpacken / Quark
// sound und performance check
// source consumed and type influence sound parameters (level of excitement?)
// not birds anymore, abstract organisms that emit sound (virtual sonocytes)
// account for presence of other birds, as "source"
// continuous update of brain
// notes <2009-02-16 Mon>
// test corner cases
// test square with odd length
// test small squares
~world = Task {
	var birds, birdsel, birdctl;
	// synth IDs
	var w1, w2, w2_num, w3;
	var asdec, noise, noisebus;
	var run = true;
	//var filenums = ["081118_165804", "081118_170905", "081118_171152"];
	//birds = Array.new(0);
	//birdsel = [200, 201]; //[200, 201, 202, 203, 204, 212];
	//filenums.do {|filenum|
	// birdsel-1.dat: all types, sel-2.dat: test single bird with brain
	birds = FileReader.readInterpret("/home/lib/code/supercollider/birds/birdsel-1.dat", true, true);
	//};
	["num bird types", birds.size].postln;
	//birds = birds[birdsel];
	//birds.size.postln;

	// ambisonic decoder
	asdec = s.nextNodeID;
	s.sendBundle(nil, ["/s_new", "asdec", asdec, 1, 0, \inchan, ~asbus.index]);

	// water
	w1 = Synth.new(\water_t, [\size, 1, \ffreq, 100.0, \fq, 0.1, \amp, 0.4]);
	w2_num = 2; // number of water_fs
	w2 = Array.new(w2_num);
	w2_num.do {|i|
		w2 = w2.add(Synth.tail(0, \water_f,	[\noisebus,
			~noisebus.index, \outc, i % 2, \dens, 30, \fwidth, 1800,
			\foffs, 60, \fq, 0.001, \amp, 0.005, \pos, 1.0.rand2]))
	};
	w3 = Synth(\stream5, [\out, ~asbus.index, \amp, -36.dbamp, \az, 0.2.rand2]);

	// noise / rumble
	noise = Synth.head(0, \noise, [\outbus, ~noisebus.index, \amp, 0.7]);

	"pre draw".postln;
	// draw bird scenario
	if(~do_gfx, {
		~drawbirds.value;
		// ~win.onClose = { run = false };
	});
	"post draw".postln;
	// birds: iterate birds
	//birds.do {|ind, i|
	//birds[[]].do {|ind, i|
	~birds_max.do {|i|
		var ind;
		//Task {
		ind = birds.choose;
		["bird picked", ind].postln;
		~add_bird.value(~asbus.index, ~num_src_type, ind);
		//(ind[1] + rrand(birdctl[i][1], birdctl[i][2])).wait;
		//(birdctl[i][3].choose + rrand(birdctl[i][1], birdctl[i][2])).wait;
	};
	//~win.refresh;
	// crickets
	// XXX
	// update world
    {
		var bx, by;
		var rhomax;
		rhomax = ((~width/2).squared + (~height/2).squared).sqrt;
		while { run } {
			//["one loop"].postln;
			~birds_alive.keys.do {|key|
				//key.postln;
				// update brain for this bird / synth / key
				~brain_update.value(key);
				//["brain post", key].postln;
				if(~birds_alive[key] != nil, {
					bx = ~birds_alive[key][0];
					by = ~birds_alive[key][1];
					//["coord:", bx, by, Complex(bx, by).theta, (1-(Complex(bx, by).rho/rhomax)).squared].postln;
					// update PanB2 in birdsynth by coordinate
					s.sendBundle(nil,
						["/n_set", key, "az", Complex(bx, by).theta/pi],
						["/n_set", key, "gn", (1-(Complex(bx, by).rho/rhomax)).squared]
					);
				});
			};
			if(~do_gfx, {
				// ~win.refresh;
			}, nil);
			0.5.wait
		}
	}.fork; // (AppClock);
};
~world.play;
)


( // record



s.recChannels_(2);
s.recHeaderFormat_("WAV");
s.recSampleFormat_("int16");

s.recChannels();
s.recHeaderFormat();
s.recSampleFormat();

s.prepareForRecord();

Routine {
	var rdur;
	rdur = 60*5.7;
	"a".postln;
	2.wait;
	s.record;
	["waiting", rdur, "seconds"].postln;
	rdur.wait;
	"b".postln;
	s.stopRecording();
}.play;

)
