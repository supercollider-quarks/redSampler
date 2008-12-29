//redFrik

//todo:
//* colours and font from skin
//* avoid n_set node not found when setting volume

RedDiskInPlayer {
	var <sampler, <isPlaying= false, <soundFiles, <win,
		playIndex, bgcol, fgcol, incdecTask,
		incView, decView, progressView, infoView, volNumView, volSldView,
		envNumView, envSldView, listView, busView, loopView, filterView;
	*new {|server, bus= 0, numItems= 10|
		^super.new.initRedDiskInPlayer(server, bus, numItems);
	}
	initRedDiskInPlayer {|argServer, argBus, argNumItems|
		var
			server= argServer ?? Server.default,
			w= 160,								//widget max width
			h= 18,								//widget height
			fnt= GUI.font.new("Monaco", 9),			//later from skin
			volSpec= [-90, 6, \db].asSpec;
		
		bgcol= Color.red(0.8);						//later from skin
		fgcol= Color.black;						//later from skin
		soundFiles= [];
		
		win= GUI.window.new(this.class.name, Rect(500, 200, w+10, h*15), false);
		win.alpha_(0.9);
		win.view.background= bgcol;
		win.view.decorator= FlowLayout(win.view.bounds);
		
		volNumView= GUI.numberBox.new(win, Rect(0, 0, w*0.25, h))
			.boxColor_(bgcol)
			.typingColor_(Color.white)
			.value_(0)
			.action_({|view|
				volSldView.value= volSpec.unmap(view.value);
				if(isPlaying, {sampler.amp= volNumView.value.dbamp});
			});
		volSldView= GUI.slider.new(win, Rect(0, 0, w*0.6, h))
			.knobColor_(fgcol)
			.value_(volSpec.unmap(0))
			.action_({|view|
				volNumView.value= volSpec.map(view.value).round(0.1);
				if(isPlaying, {sampler.amp= volNumView.value.dbamp});
			});
		GUI.staticText.new(win, Rect(0, 0, GUI.stringBounds("vol", fnt).width, h))
			.string_("vol");
		win.view.decorator.nextLine;
		
		envNumView= GUI.numberBox.new(win, Rect(0, 0, w*0.25, h))
			.boxColor_(bgcol)
			.typingColor_(Color.white)
			.value_(0.05)
			.action_({|view|
				view.value= view.value.max(0);
				envSldView.value= (view.value/10).min(1);
			});
		envSldView= GUI.slider.new(win, Rect(0, 0, w*0.6, h))
			.knobColor_(fgcol)
			.action_({|view|
				envNumView.value= (view.value*10).round(0.1);
			});
		GUI.staticText.new(win, Rect(0, 0, GUI.stringBounds("env", fnt).width, h))
			.string_("env");
		win.view.decorator.nextLine;
		
		busView= GUI.numberBox.new(win, Rect(0, 0, w*0.25, h))
			.boxColor_(bgcol)
			.typingColor_(Color.white)
			.value_(argBus)
			.action_({|view|
				view.value= view.value.asInteger.max(0);
			});
		GUI.staticText.new(win, Rect(0, 0, GUI.stringBounds("bus", fnt).width, h))
			.string_("bus");
		win.view.decorator.shift(10, 0);
		loopView= GUI.button.new(win, Rect(0, 0, w*0.4, h))
			.states_([["loop", fgcol, Color.clear], ["loop", bgcol, fgcol]]);
		win.view.decorator.nextLine;
		
		win.view.decorator.shift(10, 0);
		incView= GUI.staticText.new(win, Rect(0, 0, w*0.4, h)).string_("0:00");
		win.view.decorator.shift(10, 0);
		decView= GUI.staticText.new(win, Rect(0, 0, w*0.4, h)).string_("0:00.0");
		win.view.decorator.nextLine;
		
		progressView= GUI.multiSliderView.new(win, Rect(0, 0, w, h))
			.indexIsHorizontal_(false)
			.editable_(false)
			.indexThumbSize_(h)
			.valueThumbSize_(0)
			.isFilled_(true)
			.canFocus_(false)
			.value_([0]);
		win.view.decorator.nextLine;
		
		infoView= GUI.staticText.new(win, Rect(0, 0, w, h));
		win.view.decorator.nextLine;
		
		listView= GUI.listView.new(win, Rect(0, 0, w, h*argNumItems))
			.hiliteColor_(bgcol)
			.selectedStringColor_(Color.white)
			.action_({|view|
				this.prUpdateInfo(view.value);
				if(isPlaying, {
					this.prStopFunc(view);
				});
			})
			.enterKeyAction_({|view|
				if(soundFiles[view.value].notNil, {
					if(isPlaying, {
						this.prStopFunc(view);
					}, {
						this.prPlayFunc(view);
					});
				});
			});
		win.view.decorator.nextLine;
		
		GUI.button.new(win, Rect(0, 0, w*0.4, h))
			.states_([["folder...", fgcol, Color.clear]])
			.action_({
				if(sampler.notNil, {sampler.free});
				GUI.dialog.getPaths({|x|
					soundFiles= SoundFile.collect(PathName(x[0]).pathOnly++"*");
					if(filterView.value>0, {
						soundFiles= soundFiles.select{|x| x.numChannels==filterView.value};
					});
					soundFiles.do{|x, i|
						sampler.prepareForPlay(i, x.path);
					};
					listView.items= soundFiles.collect{|x| PathName(x.path).fileName};
					this.prUpdateInfo(0);
				});
				listView.focus;
			}).focus;
		win.view.decorator.shift(10, 0);
		filterView= GUI.numberBox.new(win, Rect(0, 0, w*0.2, h))
			.boxColor_(bgcol)
			.typingColor_(Color.white)
			.value_(0)
			.action_({|view|
				view.value= view.value.max(0).round;
			});
		GUI.staticText.new(win, Rect(0, 0, GUI.stringBounds("filter", fnt).width, h))
			.string_("filter");
		
		win.view.children.do{|x| if(x.respondsTo('font_'), {x.font_(fnt)})};
		win.bounds= win.bounds.setExtent(win.bounds.width, win.view.decorator.currentBounds.height+4);
		CmdPeriod.doOnce({if(win.isClosed.not, {win.close})});
		win.onClose= {incdecTask.stop; sampler.free};
		win.front;
		
		Routine.run{
			var halt= Condition.new;
			server.bootSync(halt);
			sampler= RedDiskInSampler(server);
			server.sync(halt);
		};
	}
	bus {
		^busView.value;
	}
	free {
		win.close;
	}
	
	//--private
	prPlayFunc {|view|
		isPlaying= true;
		playIndex= view.value;
		view.selectedStringColor_(Color.red);
		view.hiliteColor_(fgcol);
		sampler.play(
			playIndex,
			envNumView.value,
			nil,
			envNumView.value,
			volNumView.value.dbamp,
			busView.value,
			nil,
			loopView.value
		);
		incdecTask.stop;
		incdecTask= Routine({
			var startTime= SystemClock.seconds;
			var stopTime= sampler.length(playIndex);
			decView.string= stopTime.asTimeString;
			inf.do{
				var now= SystemClock.seconds-startTime;
				incView.string= now.round.asTimeString;
				10.do{
					0.1.wait;
					now= SystemClock.seconds-startTime;
					progressView.value= [(now/stopTime).min(1)];
				};
			};
		}).play(AppClock);
	}
	prStopFunc {|view|
		isPlaying= false;
		if(sampler.playingKeys.notEmpty, {
			sampler.stop(playIndex, envNumView.value);
		});
		incdecTask.stop;
		view.selectedStringColor_(Color.white);
		view.hiliteColor_(bgcol);
		progressView.value= #[0];
		incView.string= "0:00";
	}
	prUpdateInfo {|index|
		var sf= soundFiles[index];
		if(sf.notNil, {
			decView.string= sf.duration.asTimeString(0.01);
			infoView.string= "".scatList([
				sf.numChannels,
				sf.headerFormat,
				sf.sampleFormat,
				sf.sampleRate
			]);
		});
	}
}
/*
RedDiskInPlayer.new(bus:1, numItems:20)
*/