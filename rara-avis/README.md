rara avis
=========

This is the supercollider code i did for EVOL's rare birds installation project in 2008-2009, provided as is.

I'm using the Formlet UGen with an array argument for resonant frequencies as the basic synth. The first stage was to create different bird species using an evolutionary algorithm with a primitive sonic diversity fitness.

Solutions from the first stage are then selected to populate the scenario with about 20 different species. The model consists of an arena where agents are spawned, randomly selecting the species. The agent's move around in the arena hunting vital resources, following a simple brain equation for spatial adaption. The 2D location of each bird is approximately translated into a sound source location in the stereo field.
