package me.scidev5.xmodtools.player;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.data.Instrument;
import me.scidev5.xmodtools.data.Pattern;
import me.scidev5.xmodtools.data.Pattern.PatternData;
import me.scidev5.xmodtools.data.Song;

public class XMAudioController {
	private State state;
	
	private long sampleNow = 0;
	private long sampleNext = 0;
	/** If the XMAController should automatically tick itself. */
	public boolean autoTick = true;
	
	private boolean isRendering = false;
	private Thread renderThread = null;

	public final Song song;
	public final AudioFormat format;
	
	private final List<XMAudioChannel> channels;
	
	public XMAudioController(Song song, AudioFormat format) {
		if (song == null) throw new IllegalArgumentException("song was null.");
		if (format == null) throw new IllegalArgumentException("format was null.");
		
		this.song = song;
		this.format = format;
		this.state = new State(song);
		this.channels = new ArrayList<>();
		for (int i = 0; i < song.getNumChannels(); i++)
			channels.add(new XMAudioChannel(this));
		
		// Play state init.
		this.state.bpm = this.song.getDefaultBPM();
		this.state.speed = this.song.getDefaultSpeed();
		this.state.pattern = this.song.getPatternIndexByOrder(0);
	}

	/**
	 * Trigger a tick indirectly
	 */
	public void asyncTick() {
		if (isRendering)
			renderThread.interrupt();
		else 
			tick();
	}
	
	private void tick() {
		for (int i = 0; i < song.getNumChannels(); i++) {
			XMAudioChannel channel = this.channels.get(i);
			channel.preTick(this.state.tick);
		}
		if (this.state.tick == 0) {
			if (this.state.row >= this.song.getPattern(this.state.pattern).getNRows()) {
				this.state.row = 0;
				this.state.patternTableIndex++;
				if (this.state.patternTableIndex > this.song.getSongLength())
					this.state.patternTableIndex = this.song.getSongLoopPos();
				this.state.pattern = this.song.getPatternIndexByOrder(this.state.patternTableIndex);
			}
			
			Pattern pattern = song.getPattern(this.state.pattern);
			for (int i = 0; i < song.getNumChannels(); i++) {
				XMAudioChannel channel = this.channels.get(i);
				PatternData data = pattern.getData(this.state.row, i);
				
				Instrument instrument = this.song.getInstrument(data.instrument-1);
				boolean noteValid = data.note >= Constants.NOTE_FIRST && data.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT;
				
				if (data.instrument > 0 && instrument == null)
					channel.cut();
				else if (!noteValid && instrument != null)
					channel.resetNote();
				else if (channel.canNotePlayImmediately(data)) {
					if (instrument != null)
						channel.playNote(data.note-Constants.NOTE_FIRST, instrument);
					else 
						channel.switchNote(data.note-Constants.NOTE_FIRST);
				} else if (data.note == Constants.NOTE_KEYOFF)
					channel.noteOff();
				else if (channel.isNotePorta(data) && channel.hasSample() && data.instrument > 0)
					channel.resetNote();
				
				channel.runVolumeColumn(data.volume);
				channel.runEffect(data.effectType, data.effectData);
				this.runGlobalEffect(data.effectType, data.effectData);
			}

			
			this.state.row++;
		}
		for (int i = 0; i < song.getNumChannels(); i++) {
			XMAudioChannel channel = this.channels.get(i);
			channel.tick(this.state.tick);
		}
		this.state.tick++;
		this.state.tick %= this.state.speed;
		postTickTimingSetup();
	}
	private void postTickTimingSetup() {
		sampleNow = 0;
		sampleNext = (long) (2.5f/this.state.bpm*this.format.getSampleRate());
	}
	
	public void render(short[] dataL, short[] dataR) {
		double fac = 0.5;
		
		renderThread = Thread.currentThread();
		isRendering = true;
		for (int i = 0; i < dataL.length; i++) {
			double valueL = 0; double valueR = 0;
			for (XMAudioChannel channel : this.channels) {
				double value = channel.sample();
				valueL += Math.max(0, 1-channel.getPanning()) * value;
				valueR += Math.max(0, 1+channel.getPanning()) * value;
			}
			
			valueL = Math.max(-1, Math.min(1, valueL*fac));
			valueR = Math.max(-1, Math.min(1, valueR*fac));
			
			dataL[i] = (short)((valueL/2+0.5) * 0xffff - 0x8000);
			dataR[i] = (short)((valueR/2+0.5) * 0xffff - 0x8000);
			
			if (Thread.currentThread().isInterrupted() || sampleNow++ > sampleNext) tick();
		}
		isRendering = false;
		if (Thread.currentThread().isInterrupted()) tick();
	}
	
	private void runGlobalEffect(byte effectType, byte effectData) {
		// TODO
	}
	
	
	private class State {
		protected int tick = 0;
		protected int row = 0;
		protected int pattern = 0;

		protected int patternTableIndex = 0;

		private int bpm = 125;
		private int speed = 6;
		// TODO
		
		public State(Song song) {
			// TODO
		}
	}
}
