package me.scidev5.xmodtools.player;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import me.scidev5.xmodtools.data.Pattern;
import me.scidev5.xmodtools.data.Pattern.PatternData;
import me.scidev5.xmodtools.player.util.EffectType;
import me.scidev5.xmodtools.data.Song;

public class XMAudioController {
	private State state;
	
	private long sampleNow = 0;
	private long sampleNext = 0;
	/** If the XMAController should automatically tick itself. */
	public boolean autoTick = true;
	
	private float sampleVolumeScale = 0.5f;
	
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
		this.state = new State();
		this.channels = new ArrayList<>();
		for (int i = 0; i < song.getNumChannels(); i++)
			channels.add(new XMAudioChannel(this));
		
		// Play state init.
		this.state.pattern = this.song.getPatternIndexByOrder(0);
		
		this.state.setTempo(this.song.getDefaultBPM(), this.song.getDefaultSpeed());
	}


	/**
	 * Set the volume factor that is multiplied by all samples to prevent audio clipping.
	 * @param volumeScale The factor to multiply all samples by. (Range 0 (mute) - 1 (full volume))
	 */
	public void setVolumeScale(float volumeScale) {
		this.sampleVolumeScale = volumeScale;
	}
	/**
	 * Get the volume factor that is multiplied by all samples to prevent audio clipping.
	 * @return The factor to multiply all samples by.
	 */
	public float getVolumeScale() {
		return this.sampleVolumeScale;
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
		this.state.incrementPosition();
		int tick = this.state.getTick();
		
		for (XMAudioChannel channel : this.channels)
			channel.preTick();
		
		if (tick == 0) {
			this.state.globalVolumeSlide = 0;
			
			Pattern pattern = song.getPattern(this.state.getPatternIndex());
			for (int i = 0; i < song.getNumChannels(); i++) {
				PatternData data = pattern.getData(this.state.getRow(), i);
				this.runGlobalEffect(data.effectType, data.effectData);
				XMAudioChannel channel = this.channels.get(i);
				channel.runRow(data);
			}		
		}
		
		this.state.globalVolume = Math.max(0, Math.min(0x40, this.state.globalVolume + this.state.globalVolumeSlide));
		
		for (int i = 0; i < song.getNumChannels(); i++) {
			XMAudioChannel channel = this.channels.get(i);
			channel.tick(tick);
		}
		postTickTimingSetup();
	}
	private void postTickTimingSetup() {
		sampleNow = 0;
		sampleNext = (long) (2.5f/this.state.getBPM()*this.format.getSampleRate());
	}
	
	private int globalVolume() {
		if (this.state.lastGlobalVolume == -1)
			return this.state.globalVolume;
		this.state.lastGlobalVolume += Math.signum(this.state.globalVolume - this.state.lastGlobalVolume);
		return this.state.lastGlobalVolume;
	}
	/**
	 * Calculate the next chunk of audio data.
	 * @param dataL An array to hold the data for the left channel.
	 * @param dataR An array to hold the data for the right channel.
	 * @throws IllegalArgumentException If the data channels have different lengths.
	 */
	public void render(short[] dataL, short[] dataR) throws IllegalArgumentException {
		if (dataL.length != dataR.length)
			throw new IllegalArgumentException("Audio channel arrays had different lengths!");
		
		double fac = 0.75*this.sampleVolumeScale * Math.max(0f, Math.min(1f, this.globalVolume() / (float)0x40));
		
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
			
			if (Thread.currentThread().isInterrupted() || sampleNow++ > sampleNext) {
				tick();
				fac = 0.75*this.sampleVolumeScale * Math.max(0f, Math.min(1f, this.globalVolume() / (float)0x40));
			}
		}
		isRendering = false;
		if (Thread.currentThread().isInterrupted()) tick();
	}
	
	private void runGlobalEffect(byte effectType, byte effectData) {
		EffectType type = EffectType.get(effectType, effectData);
		
		int data = effectData & 0xff;
		
		if (type.dataId != -1)
			data = data & 0xf;
		
		switch (type) {

		case SET_TEMPO: 
			if (data > 0) {
				if (data >= 0x20)
					this.state.setBPM(data);
				else 
					this.state.setSpeed(data);
			}
			break;
		case JUMP_TO_ORDER: 
			this.state.setPatternTablePosition(0,data);
			break;
		case DELAY_ROW: 
			this.state.setDelay(data);
			break;
		case PATTERN_LOOP: 
			if (data == 0) {
				this.state.setRestartPos(this.state.getRow());
			} else if (this.state.loopsLeft > 0) {
				this.state.loopsLeft--;
				if (this.state.loopsLeft > 0)
					this.state.jumpRestartPos();
			} else {
				this.state.loopsLeft = data;
				this.state.jumpRestartPos();
			}
			break;
		case PATTERN_BREAK: 
			this.state.setPatternTablePosition(data, this.state.getPatternTableIndex() + 1);
			break;
		case SET_GLOBAL_VOLUME: 
			this.state.globalVolume = Math.max(0,Math.min(0x40, data));
			if (this.state.lastGlobalVolume == -1)
				this.state.lastGlobalVolume = this.state.globalVolume;
			break;
		case GLOBAL_VOLUME_SLIDE: 
			if (this.state.lastGlobalVolume == -1)
				this.state.lastGlobalVolume = this.state.globalVolume;
			
			if (data == 0) data = this.state.memory_volumeSlide;
			else           this.state.memory_volumeSlide = data;

			int down = data & 0xf;
			int up = (data >> 4) & 0xf;
			
			this.state.globalVolumeSlide = up > 0 ? up : -down;
			break;
		case GLISSANDO_CONTROL: 
			if (data == 0)      this.state.quantizePorta = false;
			else if (data == 1) this.state.quantizePorta = true;
			break;
		
		default:
			break;
		}
		
		if (this.state.lastGlobalVolume == -1)
			this.state.lastGlobalVolume = this.state.globalVolume;
	}
	
	/**
	 * Get the state object for this controller.
	 * @return The dynamic state of this controller.
	 */
	public State getState() {
		return this.state;
	}
	
	
	public class State {
		// POSITION
		protected int tick = -1;
		protected int row = -1;
		protected int rowNext = -1;
		protected int pattern = 0;
		protected int patternTableIndex = 0;
		
		protected int delay = 0;
		
		protected int restartPosition = 0;
		protected boolean defaultToRestart = false;
		
		private void incrementPosition() {
			Song song = XMAudioController.this.song;
			
			this.tick++;
			this.tick %= this.speed * (Math.max(0, this.delay) + 1);

			if (this.tick == 0) {
				this.delay = 0;
				if (this.rowNext >= 0) {
					this.row = this.rowNext;
					this.rowNext = -1;
				} else
					this.row++;
				
				if (this.row >= song.getPattern(this.pattern).getNRows()) {
					this.setPatternTablePosition(this.getPatternTableIndex() + 1);
					this.row = this.rowNext;
					this.rowNext = -1;
				}
			}
		}

		/**
		 * Set the position in the patternTable for the next row.
		 * (If the index is greater than or equal to the song length
		 * the song will loop back to its restart pattern table index)
		 * @param row The row from which to start the pattern.
		 * @param patternTableIndex The index in the patternTable to set.
		 */
		public void setPatternTablePosition(int row, int patternTableIndex) {
			this.rowNext = row;
			this.restartPosition = 0;
			this.defaultToRestart = false;
			this.patternTableIndex = patternTableIndex;
			
			if (this.patternTableIndex >= XMAudioController.this.song.getSongLength())
				this.patternTableIndex = XMAudioController.this.song.getSongLoopPos();
			
			this.pattern = XMAudioController.this.song.getPatternIndexByOrder(this.patternTableIndex);
			if (this.rowNext >= XMAudioController.this.song.getPattern(this.pattern).getNRows() || this.rowNext < 0)
				this.rowNext = 0;
		}
		/**
		 * Set the position in the patternTable for the next row.
		 * (If the index is greater than or equal to the song length
		 * the song will loop back to its restart pattern table index)
		 * @param patternTableIndex The index in the patternTable to set.
		 */
		public void setPatternTablePosition(int patternTableIndex) {
			this.setPatternTablePosition(this.defaultToRestart?this.restartPosition:0, patternTableIndex);
		}
		/**
		 * Set the next row to play to be restartPos. 
		 * (!!INFO!! Next pattern will start on restartPos if this function is called.)
		 */
		public void jumpRestartPos() {
			this.rowNext = this.restartPosition;
			this.defaultToRestart = true;
			if (this.rowNext >= XMAudioController.this.song.getPattern(this.pattern).getNRows() || this.rowNext < 0)
				this.rowNext = 0;
		}
		/**
		 * Set where to loop back to when calling jumpRestartPos().
		 * @param restartPos The row to jump to on restart.
		 */
		public void setRestartPos(int restartPos) {
			this.restartPosition = restartPos;
		}
		/**
		 * Set how many times longer the current row will take.
		 * @param delay The delay (in row lengths) until the next row.
		 */
		public void setDelay(int delay) {
			this.delay = Math.max(0, delay);
		}

		/**
		 * Force the next row to start playing.
		 */
		public void forcePlayNextRow() {
			this.tick = -1;
			XMAudioController.this.asyncTick();
		}
		
		/**
		 * Get the current tick of the player controller.
		 * @return The current tick.
		 */
		public int getTick() {
			return this.tick;
		}
		/**
		 * Get the current index of the player controller in the pattern table.
		 * @return The current patternTableIndex. 
		 */
		public int getPatternTableIndex() {
			return this.patternTableIndex;
		}
		/**
		 * Get the index of the current playing pattern.
		 * @return The index of the playing pattern.
		 */
		public int getPatternIndex() {
			return this.pattern;
		}
		/**
		 * Get the current row number in the pattern.
		 * @return The current row.
		 */
		public int getRow() {
			return this.row;
		}
		
		// TEMPO
		protected int bpm = 125;
		protected int speed = 6;

		/**
		 * Set the speed and bpm of the controller.
		 * @param bpm Tick speed: 2500ms / bpm. (Range: $20 - $ff)
		 * @param speed Number of ticks per row. (Range: $0 - $1f)
		 */
		public void setTempo(int bpm, int speed) {
			this.setSpeed(speed);
			this.setBPM(bpm);
		}
		/**
		 * Set the speed of the controller.
		 * @param speed Number of ticks per row. (Range: $0 - $1f)
		 */
		public void setSpeed(int speed) {
			this.speed = Math.min(0x1f, Math.max(0, speed));
		}
		/**
		 * Set the bpm of the controller.
		 * @param bpm Tick speed: 2500ms / bpm. (Range: $20 - $ff)
		 */
		public void setBPM(int bpm) {
			this.bpm = Math.min(0xff, Math.max(0x20, bpm));
		}
		/** 
		 * Get the current speed of the controller. 
		 * @return Number of ticks per row. (Range: $0 - $1f) 
		 */
		public int getSpeed() { return this.speed; }
		/** 
		 * Get the current bpm of the controller. 
		 * @return Tick speed: 2500ms / bpm. (Range: $20 - $ff) 
		 */
		public int getBPM() { return this.bpm; }
		
		
		// EFFECT DATA 
		
		private int memory_volumeSlide = 0;
		private int globalVolume = 0x40;
		private int lastGlobalVolume = -1;
		private int globalVolumeSlide = 0;
		
		private int loopsLeft = 0;
		private boolean quantizePorta = false;
		
		/**
		 * Returns whether or not the portamento should round to the nearest half-step.
		 */
		public final boolean doQuantizePorta() { return this.quantizePorta; }
	}
}
