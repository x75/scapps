
// nupg data main controller, non-GUI
// data is accessed via the data dict
~data;
~data.data_main;
~data.data_main[0];
~data.data_main[0][0];
~data.data_main[0][0].value;
~data.data_main[0][0].value = 80;
~data.data_main[0][0].size;

~data.data_pulsaret[0]


//n = NetAddr("127.0.0.1", 57120); // local machine

(
thisProcess.openUDPPort(9000);

// some functions
~createRandomMatrix = {|rows=1, cols=1, range=1.0|
	Matrix.fill(rows, cols, {|row, col| range.rand});
};

~makeMatrixSparse = {|mtx, sparsity=0.9|
	mtx.doMatrix({|item, row, col|
		if(1.0.rand < sparsity,
			{mtx.put(row, col, 0)});
	});
	mtx
};

// custom soundfiles
~soundfilelist = File.new(Platform.userHomeDir +/+ "nnoi/soundfiles.txt", "r");
//[~soundfilelist].postln;
~soundfilelist.seek(0);
~soundfiles = ~soundfilelist.readAllString.split($\n); //.postln;
~soundfiles.removeAt(~soundfiles.size-1);
[~soundfiles.size, ~soundfiles].postln;
~soundfilelist.close;

// nupg default tables
~xfiles = {|tablePath| ["/*.wav", "/*.aiff"].collect{|item|  (tablePath ++ item).pathMatch}.flatten };
// ~xfileNames = ~xfiles.value(~path).collect{|i| PathName(i).fileName};
~xfileNames = ~xfiles.value(~path).collect{|i| i};

// load data into pulsarets buffers
~loadDataPulsaret = {
	var path, file, temp, array;
	~data.data_pulsaret.size.do {|i|
		["reload Pulsaret"].postln;
		path = ~soundfiles.choose; // "/home/x75/res_out_01_trim.wav";
		[\path, path].postln;
		file = SoundFile.new;
		file.openRead(path);
		temp = FloatArray.newClear(4096);
		file.readData(temp);
		array = temp.asArray.resamp1(2048).copy;
		//array = array.linlin(-1.0, 1.0, -1.0, 1.0);
		~data.data_pulsaret[i].value_(array);
		~buffers[i][0].sendCollection(array);
	};
};
~loadDataPulsaret.value();

// load data into envelope buffers
~loadDataEnvelope = {
	var path, file, temp, array;
	~data.data_envelope.size.do {|i|
		["reload Envelope"].postln;
		path = ~xfileNames.choose; // "/home/x75/res_out_01_trim.wav";
		[\path, path].postln;
		file = SoundFile.new;
		file.openRead(path);
		temp = FloatArray.newClear(4096);
		file.readData(temp);
		array = temp.asArray.resamp1(2048).copy;
		//array = array.linlin(-1.0, 1.0, -1.0, 1.0);
		~data.data_envelope[i].value_(array);
		~buffers[i][1].sendCollection(array);
	};
};
~loadDataEnvelope.value();

// load data into frequency buffers
~loadDataFrequency = {
	var path, file, temp, array;
	~data.data_frequency.size.do {|i|
		["reload Frequency"].postln;
		path = ~xfileNames.choose; // "/home/x75/res_out_01_trim.wav";
		[\path, path].postln;
		file = SoundFile.new;
		file.openRead(path);
		temp = FloatArray.newClear(4096);
		file.readData(temp);
		array = temp.asArray.resamp1(2048).copy;
		//array = array.linlin(-1.0, 1.0, -1.0, 1.0);
		~data.data_frequency[i].value_(array);

		~buffers[i][2].sendCollection(array);
	};
};
~loadDataFrequency.value();
)

// ~data.data_pulsaret
// ~data.data_envelope
// // ~buffers[0][0];
// ~tables
// ~path

// little plan
// x1 load soundfiles and get the trains playing
// 2 create matrix that maps inputs onto controler state
// 3

(
// start all trains
NuPG_Ndefs.trains[0].play;
NuPG_Ndefs.trains[1].play;
NuPG_Ndefs.trains[2].play;
)

(
// stop all trains
NuPG_Ndefs.trains[0].stop;
NuPG_Ndefs.trains[1].stop;
NuPG_Ndefs.trains[2].stop;
)

(
// ~xdata_main1 = Matrix.fill(15, 1, {|row, col| 1.0.rand});
~flatcat_xdata = Matrix.fill(3, 5, {|row, col| 0.0});
~acc_xdata = Matrix.fill(3, 5, {|row, col| 0.0});
~mag_xdata = Matrix.fill(3, 5, {|row, col| 0.0});
~gyr_xdata = Matrix.fill(3, 5, {|row, col| 0.0});
// ~xdata_prox = Matrix.fill(3, 5, {|row, col| 0.0});
// ~xdata_light = Matrix.fill(3, 5, {|row, col| 0.0});
~player1_xdata = Matrix.fill(3, 5, {|row, col| 0.0});

~sum_xdata = Matrix.fill(3, 5, {|row, col| 0.0});


~numdata = 0.5;

// copy state to nupg control
~numdata = 2.0;
~update_interval = 0.05;
~xstate2control = Task({
	inf.do({|i|
		~sum_xdata = ~sum_xdata * 0.0;
		~sum_xdata = ~sum_xdata + (~numdata.reciprocal * ~flatcat_xdata);
		~sum_xdata = ~sum_xdata + (~numdata.reciprocal * ~acc_xdata);
		~sum_xdata = ~sum_xdata + (~numdata.reciprocal * ~mag_xdata);
		~sum_xdata = ~sum_xdata + (~numdata.reciprocal * ~gyr_xdata);
		~sum_xdata = ~sum_xdata + (~numdata.reciprocal * ~player1_xdata);

		// copy sum data to nupg data
		~sum_xdata.rows.do({|row|
			~sum_xdata.cols.do({|col|
				~data.data_main[row][col].input = ~sum_xdata.get(row, col);
			});
		});
		~update_interval.wait;
	});
});
)

(
~xstate2control.play;
)

(
~xstate2control.stop;
~xstate2control.free;
);

(
// reload data
~loadDataFuncs = [~loadDataPulsaret, ~loadDataEnvelope, ~loadDataFrequency];
~loadOSCproximity = {
	OSCdef(\proximity, {|msg, time, addr, recvPort|
		msg.postln;
		if(msg[1] == 0.0,
			{
				~loadDataFuncs.choose.value();
		});
	}, '/proximity', nil); // def style
};

~unloadOSCproximity = {
	OSCdef(\proximity).free;
};

~loadOSCproximity.value();
)

(
// flatcat2nupg
~loadOSCflatcat2nupg = {
	OSCdef(\flatcat2nupg, {|msg, time, addr, recvPort|
		msg.postln;
		~data.data_main.size.do({|row|
			// row.postln;
			~data.data_main[row].size.do({|col|
				// [row, col].postln;
				// msg.size.postln;
				// ((row * 5) + col + 1).postln;
				// msg[((row * 5) + col + 1)].postln;
				// msg[].postln;
				// ~data.data_main[row][col].input = ~flatcat_y.get(row * ~flatcat_M.cols + col, 0);
				~flatcat_xdata.put(row, col, msg[(row * 5) + col + 1]);
			});
		});
		// ~flatcat_cnt = ~flatcat_cnt + 1;
	}, '/flatcat2nupg', nil); // def style
};

~unloadOSCflatcat2nupg = {
	OSCdef(\flatcat).free;
};

~loadOSCflatcat2nupg.value();
)

~unloadOSCflatcat2nupg.value;



(
// flatcat
~loadOSCflatcat = {
	OSCdef(\flatcat, {|msg, time, addr, recvPort|
		msg.postln;
		// triggerfreq <- temperature
		~data.data_main[0][0].value = msg[2] * 0.5;
		// grainfreq <- current
		~data.data_main[0][1].value = msg[5] * 10;
		// envelope multiplication <- velocity
		~data.data_main[0][2].value = msg[4] * 10 + 1;
		// pan <- position
		~data.data_main[0][3].value = (msg[3] / pi) * 5;
		~data.data_main[1][0].value = msg[6] * 0.5;
		~data.data_main[1][1].value = msg[9] * 10;
		~data.data_main[1][2].value = msg[8] * 10 + 1;
		~data.data_main[1][3].value = (msg[7] / pi) * 5;
	}, '/flatcat', nil); // def style
};

~unloadOSCflatcat = {
	OSCdef(\flatcat).free;
};

~loadOSCflatcat.value();
)

~unloadOSCflatcat.value;

(
// flatcat new
~loadOSCflatcatnew = {
	OSCdef(\flatcat, {|msg, time, addr, recvPort|
		msg.postln;
		// triggerfreq <- temperature
		~data.data_main[0][0].value = msg[4] * 0.5;
		// grainfreq <- current
		~data.data_main[0][1].value = msg[7] * 10;
		// envelope multiplication <- velocity
		~data.data_main[0][2].value = msg[6] * 10 + 1;
		// pan <- position
		~data.data_main[0][3].value = (msg[5] / pi) * 5;

		~data.data_main[1][0].value = msg[8] * 0.5;
		~data.data_main[1][1].value = msg[11] * 10;
		~data.data_main[1][2].value = msg[10] * 10 + 1;
		~data.data_main[1][3].value = (msg[9] / pi) * 5;

		~data.data_main[2][0].value = msg[12] * 0.5;
		~data.data_main[2][1].value = msg[15	] * 10;
		~data.data_main[2][2].value = msg[14] * 10 + 1;
		~data.data_main[2][3].value = (msg[13] / pi) * 5;
	}, '/flatcat', nil); // def style
};

~unloadOSCflatcatnew = {
	OSCdef(\flatcat).free;
};

~loadOSCflatcatnew.value();
)

~unloadOSCflatcatnew.value();
//
// OSCdef.freeAll;
~buf1recorder.set(\gain, 1.3);

(
// flatcat sensor
~flatcat_cnt = 0;
~flatcat_func = [{|x| x.tanh + 1 / 2}, {|x| x.abs}].choose;
~flatcat_gain = 24.0;
~flatcat_offs = 0.7; // 0.0
//~flatcat_numdata = 8;
~flatcat_numdata = 12;
~flatcat_x = Matrix.fill(~flatcat_numdata, 1, {|row, col| 1.0.rand});
// ~flatcat_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
// ~flatcat_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
~flatcat_M = ~createRandomMatrix.value(15, ~flatcat_numdata, 1.0);
~flatcat_M = ~makeMatrixSparse.value(~flatcat_M, 0.2);
~flatcat_M = ~flatcat_M/~flatcat_M.sum();
// flatcat
OSCdef.new(\flatcat, {|msg, time, addr, recvPort|
	// msg.postln;
	// ~flatcat_x.putCol(0, msg[[1,2,3]]);
	// triggerfreq <- temperature
	// ~flatcat_x.put(0, 0, msg[2] * 0.01);

	// flatcat2D
	// // grainfreq <- current
	// ~flatcat_x.put(1, 0, msg[5] * 5.0);
	// // envelope multiplication <- velocity
	// ~flatcat_x.put(2, 0, msg[4] * 5.0);
	// // pan <- position
	// ~flatcat_x.put(3, 0, (msg[3] / pi) * 5.0);
	//
	// // temp
	// ~flatcat_x.put(4, 0, msg[6] * 0.01);
	// // current
	// ~flatcat_x.put(5, 0, msg[9] * 5.0);
	// ~flatcat_x.put(6, 0, msg[8] * 5.0);
	// ~flatcat_x.put(7, 0, (msg[7] / pi) * 5.0);

	// flatcat3D
	~flatcat_x.put(0, 0, msg[4] * 0.01);
	// grainfreq <- current
	~flatcat_x.put(1, 0, msg[7] * 10.0);
	// envelope multiplication <- velocity
	~flatcat_x.put(2, 0, msg[6] * 10.0);
	// pan <- position
	~flatcat_x.put(3, 0, (msg[5] / pi) * 10.0);

	// temp
	~flatcat_x.put(4, 0, msg[8] * 0.01);
	// current
	~flatcat_x.put(5, 0, msg[11] * 10.0);
	~flatcat_x.put(6, 0, msg[10] * 10.0);
	~flatcat_x.put(7, 0, (msg[9] / pi) * 10.0);
	// temp
	~flatcat_x.put(8, 0, msg[12] * 0.01);
	// current
	~flatcat_x.put(9, 0, msg[15] * 10.0);
	~flatcat_x.put(10, 0, msg[14] * 10.0);
	~flatcat_x.put(11, 0, (msg[13] / pi) * 10.0);

	~flatcat_x.postln;

	~flatcat_x = (~flatcat_x - ~flatcat_offs) * ~flatcat_gain;
	// ~flatcat_y = (~flatcat_M * ~flatcat_x).tanh + 1 / 2;
	// ~flatcat_y = (~flatcat_M * ~flatcat_x).abs;
	~flatcat_y = ~flatcat_func.value(~flatcat_M * ~flatcat_x);
	// (~flatcat_y).postln;
	~data.data_main.size.do({|row|
		// row.postln;
		~data.data_main[row].size.do({|col|
			// [row, col].postln;
			// ~data.data_main[row][col].input = ~flatcat_y.get(row * ~flatcat_M.cols + col, 0);
			~flatcat_xdata.put(row, col, ~flatcat_y.get(row * ~data.data_main[row].size + col, 0));
			});
		});
	~flatcat_cnt = ~flatcat_cnt + 1;
	if(~flatcat_cnt % 100 == 0, {[\flatcat, ~flatcat_cnt, ~flatcat_x, ~flatcat_y].postln;});
}, '/flatcat', nil); // def style
)

OSCdef(\flatcat).free;

// TODO
// variations: matrix sparsity, tanh scale/tanh abs
// output combination / mix

~data.sieveMasking
~masking

~data.show;
s.scope
(
// sensors2OSC accelerometer
~acc_cnt = 0;
~acc_func = [{|x| x.tanh + 1 / 2}, {|x| x.abs}].choose;
~acc_gain = 0.5;
~acc_offs = 0;
~acc_x = Matrix.fill(3, 1, {|row, col| 1.0.rand});
// ~acc_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
// ~acc_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
~acc_M = ~createRandomMatrix.value(15, 3, 1.0);
~acc_M = ~makeMatrixSparse.value(~acc_M, 0.2);
~acc_M = ~acc_M/~acc_M.sum();
OSCdef(\acc_s2O, {|msg, time, addr, recvPort|
	// msg[[1,2,3]].postln;
	~acc_x.putCol(0, msg[[1,2,3]]);
	~acc_x = (~acc_x - ~acc_offs) * ~acc_gain;
	// ~acc_y = (~acc_M * ~acc_x).tanh + 1 / 2;
	// ~acc_y = (~acc_M * ~acc_x).abs;
	~acc_y = ~acc_func.value(~acc_M * ~acc_x);
	// (~acc_y).postln;
	~data.data_main.size.do({|row|
		// row.postln;
		~data.data_main[row].size.do({|col|
			// [row, col].postln;
			// ~data.data_main[row][col].input = ~acc_y.get(row * ~acc_M.cols + col, 0);
			~acc_xdata.put(row, col, ~acc_y.get(row * ~acc_M.cols + col, 0));
		});
	});
	~acc_cnt = ~acc_cnt + 1;
	if(~acc_cnt % 10 == 0, {[\acc, ~acc_cnt, ~acc_x, ~acc_y].postln;});
}, '/accelerometer', nil); // def style
)

OSCdef(\acc_s2O).free;

(
// sensors2OSC magnetometer
~mag_cnt = 0;
~mag_func = [{|x| x.tanh + 1 / 2}, {|x| x.abs}].choose;
~mag_gain = 0.5;
~mag_offs = 0;
~mag_x = Matrix.fill(3, 1, {|row, col| 1.0.rand});
// ~mag_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
// ~mag_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
~mag_M = ~createRandomMatrix.value(15, 3, 1.0);
~mag_M = ~makeMatrixSparse.value(~mag_M, 0.5);
~mag_M = ~mag_M/~mag_M.sum();
OSCdef(\mag_s2O, {|msg, time, addr, recvPort|
	// msg[[1,2,3]].postln;
	~mag_x.putCol(0, msg[[1,2,3]]);
	~mag_x = (~mag_x - ~mag_offs) * ~mag_gain;
	// ~mag_y = (~mag_M * ~mag_x).tanh + 1 / 2;
	// ~mag_y = (~mag_M * ~mag_x).abs;
	~mag_y = ~mag_func.value(~mag_M * ~mag_x);
	// (~mag_y).postln;
	~data.data_main.size.do({|row|
		// row.postln;
		~data.data_main[row].size.do({|col|
			// [row, col].postln;
			// ~data.data_main[row][col].input = ~mag_y.get(row * ~mag_M.cols + col, 0);
			~mag_xdata.put(row, col, ~mag_y.get(row * ~mag_M.cols + col, 0));
		});
	});
	~mag_cnt = ~mag_cnt + 1;
	if(~mag_cnt % 10 == 0, {[\mag, ~mag_cnt, ~mag_x].postln;});
}, '/magneticfield', nil); // def style
)

(
// sensors2OSC gyroscope
~gyr_cnt = 0;
~gyr_func = [{|x| x.tanh + 1 / 2}, {|x| x.abs}].choose;
~gyr_gain = 0.5;
~gyr_offs = 0;
~gyr_x = Matrix.fill(3, 1, {|row, col| 1.0.rand});
// ~gyr_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
// ~gyr_M = Matrix.fill(15, 3, {|row, col| 1.0.rand});
~gyr_M = ~createRandomMatrix.value(15, 3, 1.0);
~gyr_M = ~makeMatrixSparse.value(~gyr_M, 0.5);
~gyr_M = ~gyr_M/~gyr_M.sum();
OSCdef(\gyr_s2O, {|msg, time, addr, recvPort|
	// msg[[1,2,3]].postln;
	~gyr_x.putCol(0, msg[[1,2,3]]);
	~gyr_x = (~gyr_x - ~gyr_offs) * ~gyr_gain;
	// ~gyr_y = (~gyr_M * ~gyr_x).tanh + 1 / 2;
	// ~gyr_y = (~gyr_M * ~gyr_x).abs;
	~gyr_y = ~gyr_func.value(~gyr_M * ~gyr_x);
	// (~gyr_y).postln;
	~data.data_main.size.do({|row|
		// row.postln;
		~data.data_main[row].size.do({|col|
			// [row, col].postln;
			// ~data.data_main[row][col].input = ~gyr_y.get(row * ~gyr_M.cols + col, 0);
			~gyr_xdata.put(row, col, ~gyr_y.get(row * ~gyr_M.cols + col, 0));
		});
	});
	~gyr_cnt = ~gyr_cnt + 1;
	if(~gyr_cnt % 10 == 0, {[\gyr, ~gyr_cnt, ~gyr_x].postln;});
}, '/gyroscope', nil); // def style
)

OSCdef(\acc_s2O).free;
OSCdef(\mag_s2O).free;
OSCdef(\gyr_s2O).free;

// (~acc_x/10).tanh + 1 / 2



~acc_M = ~createRandomMatrix.value(15, 3, 1.0);
~acc_M = ~makeMatrixSparse.value(~acc_M, 0.8);
~acc_M

(
// sensors2OSC 2
~acc_x = Matrix.fill(3, 1, {|row, col| 0.0});
~acc_x_mu = Matrix.fill(3, 1, {|row, col| 0.0});
~acc_x_sig = Matrix.fill(3, 1, {|row, col| 1.0});
~acc_x_std = Matrix.fill(3, 1, {|row, col| 0.0});
~acc_lr = 0.01;
~acc_y = Matrix.fill(3, 1, {|row, col| 0.0});
~acc_M = ~createRandomMatrix.value(15, 3, 1.0);
~acc_M = ~makeMatrixSparse.value(~acc_M, 0.7);

OSCdef(\s2Oacc, {|msg, time, addr, recvPort|
	// msg.postln;
	~acc_x.putCol(0, msg[[1,2,3]]);
	~acc_x_mu = (1-~acc_lr) * ~acc_x_mu + (~acc_lr * ~acc_x);
	~acc_x_sig = (1-~acc_lr) * ~acc_x_sig + (~acc_lr * (~acc_x - ~acc_x_mu).squared.sqrt);
	[\mu, ~acc_x_mu].postln;
	// [\sig, ~acc_x_sig].postln;
	~acc_x_std = (~acc_x - ~acc_x_mu);
	// ~acc_x_std = (~acc_x_std / ~acc_x_sig);
	// [\std, ~acc_x_std].postln;
}, '/accelerometer', nil); // def style
)

OSCdef(\s2Oacc).free;

// masking
(
3.do({|i|
	~data.data_probabilityMask[i][0].value = 1.0.rand;
	~data.data_burstMask[i][0].value = 10.rand;
	~data.data_burstMask[i][1].value = 10.rand;
});
)

(
3.do({|i|
	~data.data_probabilityMask[i][0].value = 1.0;
	~data.data_burstMask[i][0].value = 1; //10.rand;
	~data.data_burstMask[i][1].value = 0; //10.rand;
});
)


// ~acc_lr * ~acc_x
(1-~acc_lr) * ~acc_x_mu + (~acc_lr * ~acc_x)