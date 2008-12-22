//these classes are part of the RedSample package / redFrik, gnu gpl v2
//preloads buffer when .play so there will be a slight hickup and latency

RedDiskInSampler : RedAbstractSampler {				//playing sounds from disk
	var <>numFrames= 32768;							//preload buffer size in samples
	*initClass {
		StartUp.add{
			8.do{|i|								//change here for more channels than 8
				("SynthDef('redDiskInSampler-"++(i+1)++"', {
					|i_out= 0, bufnum, amp= 0.7, attack= 0.01, sustain, release= 0.1, gate= 1, ctrl= #"++1.dup(i+1)++", lag= 0.1|
					var src= DiskIn.ar("++(i+1)++", bufnum, 0);
					var env= EnvGen.kr(
						Env(#[0, 1, 1, 0], [attack, sustain, release], -4),
						gate,
						amp,
						0,
						1,
						2						//doneAction
					);
					Out.ar(i_out, src*env*Ramp.kr(ctrl, lag));
				}, #['ir']).store").interpret;
				("SynthDef('redDiskInSampler-"++(i+1)++"loop', {
					|i_out= 0, bufnum, amp= 0.7, attack= 0.01, release= 0.1, gate= 1, ctrl= #"++1.dup(i+1)++", lag= 0.1|
					var src= DiskIn.ar("++(i+1)++", bufnum, 1);
					var env= EnvGen.kr(
						Env(#[0, 1, 0], [attack, release], -4, 1),
						gate,
						amp,
						0,
						1,
						2						//doneAction
					);
					Out.ar(i_out, src*env*Ramp.kr(ctrl, lag));
				}, #['ir']).store").interpret;
			}
		}
	}
	prCreateVoice {|sf, startFrame|
		^RedDiskInSamplerVoice(server, sf.path, sf.numChannels, startFrame, numFrames, sf.duration);
	}
}

RedDiskInSamplerVoice : RedAbstractSamplerVoice {
	defName {^"redDiskInSampler-"++channels}
	play {|attack, sustain, release, amp, out, group, loop, ctrl|
		var name= this.defName;
		if(loop==1, {name= name++"loop"});
		isPlaying= true;
		synth= Synth.basicNew(name, server);
		buffer.cueSoundFile(path, startFrame, {
			OSCresponderNode(server.addr, '/n_end', {|t, r, m|
				if(m[1]==synth.nodeID, {
					buffer.close;
					isPlaying= false;
					isReleased= false;
					r.remove;
				});
			}).add;
			synth.addToHeadMsg(group ?? {server.defaultGroup}, [
				\i_out, out,
				\bufnum, buffer.bufnum,
				\amp, amp,
				\attack, attack,
				\sustain, sustain ?? {(length-attack-release).max(0)},
				\release, release
			]);
		});
	}
	prAllocBuffer {|action|
		buffer= Buffer.alloc(server, numFrames, channels, action)
	}
}
