// set these names to your local host name
~oscnamelocal  = "theta";
~oscnamelocals = "theta".asSymbol;
// set number busses / units
~numabus = 10;
~numcbus = 10;
~oscnameremote = (
	(~oscnamelocals): ~numcbus,
	'vrt':            ~numcbus,
	'hiaz':           ~numcbus
);
// list of available synth defs
~synthdefs = ["fm1", "chaosfb1", "chaosfb2", "chaosfb3", "chaosfb4", "chaosfb5", "chaosfb6"];
