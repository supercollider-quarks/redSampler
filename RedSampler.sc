//these classes are part of the RedSample package / redFrik, gnu gpl v2
//preloads buffer into ram

RedSampler : RedAbstractSampler {					//playing buffers in ram
	*initClass {
		StartUp.add{
			8.do{|i|								//change here for more channels than 8
				("SynthDef('redSampler-"++(i+1)++"', {
					|i_out= 0, bufnum, amp= 0.7, attack= 0.01, sustain, release= 0.1, gate= 1, offset= 0, ctrl= #"++1.dup(i+1)++", lag= 0.1|
					var src= PlayBuf.ar(
						"++(i+1)++",
						bufnum,
						BufRateScale.ir(bufnum),
						1,
						BufFrames.ir(bufnum)*offset,
						0
					);
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
				("SynthDef('redSampler-"++(i+1)++"loop', {
					|i_out= 0, bufnum, amp= 0.7, attack= 0.01, release= 0.1, gate= 1, offset= 0, ctrl= #"++1.dup(i+1)++", lag= 0.1|
					var src= PlayBuf.ar(
						"++(i+1)++",
						bufnum,
						BufRateScale.ir(bufnum),
						1,
						BufFrames.ir(bufnum)*offset,
						1
					);
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
		^RedSamplerVoice(server, sf.path, sf.numChannels, startFrame, nil, sf.duration);
	}
}

RedSamplerVoice : RedAbstractSamplerVoice {
	defName {^"redSampler-"++channels}
	play {|attack, sustain, release, amp, out, group, loop|
		var name= this.defName;
		if(loop==1, {name= name++"loop"});
		isPlaying= true;
		synth= Synth.head(group ?? {server.defaultGroup}, name, [
			\i_out, out,
			\bufnum, buffer.bufnum,
			\amp, amp,
			\attack, attack,
			\sustain, sustain ?? {(length-attack-release).max(0)},
			\release, release
		]);
		OSCresponderNode(server.addr, '/n_end', {|t, r, m|
			if(m[1]==synth.nodeID, {
				isPlaying= false;
				isReleased= false;
				r.remove;
			});
		}).add;
	}
	prAllocBuffer {|action|
		buffer= Buffer.read(server, path, startFrame, -1, action)
	}
}
