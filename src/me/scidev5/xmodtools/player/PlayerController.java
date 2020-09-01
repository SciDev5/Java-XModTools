package me.scidev5.xmodtools.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.data.Pattern;
import me.scidev5.xmodtools.data.Song;

public class PlayerController {
	private State state;
	
	private long sampleNow = 0;
	private long sampleNext = 0;
	public boolean autoTick = true;
	
	private boolean isRendering = false;
	private Thread renderThread = null;

	private final Song song;
	private final PlayerThread player;
	
	private final List<PlayerChannel> channels;
	
	public PlayerController(Song song, PlayerThread player) {
		this.song = song;
		this.player = player;
		this.state = new State(song);
		this.channels = new ArrayList<>();
		for (int i = 0; i < song.getNumChannels(); i++)
			channels.add(new PlayerChannel(this.player));
	}

	public void asyncTick() {
		if (isRendering)
			renderThread.interrupt();
		else 
			tick();
	}
	
	private void tick() {
		
		if (this.state.tick == 0) {
			Pattern pattern = song.getPattern(0);
			for (int i = 0; i < song.getNumChannels(); i++) {
				PlayerChannel channel = this.channels.get(i);
				int note = pattern.getData(this.state.row, i).note;
				if (note >= Constants.NOTE_FIRST && note < Constants.NOTE_FIRST + Constants.NOTE_COUNT) {
					System.out.println(note);
					channel.playNote(note-1, this.song.getInstrument(0));
				} else if (note == Constants.NOTE_KEYOFF) {
					System.out.println("off");
				}
			}
			
			this.state.row++;
			this.state.row %= pattern.getNRows();
		}
		this.state.tick++;
		this.state.tick %= 6;
		postTickTimingSetup();
	}
	private void postTickTimingSetup() {
		sampleNow = 0;
		sampleNext = (long) (0.02f*player.getSampleRate());
	}
	
	public void render(short[] dataL, short[] dataR) {
		renderThread = Thread.currentThread();
		isRendering = true;
		for (int i = 0; i < dataL.length; i++) {
			double valueL = 0; double valueR = 0;
			for (PlayerChannel channel : this.channels) {
				double value = channel.sample();
				valueL += Math.max(0, 1-channel.getPanning()) * value;
				valueR += Math.max(0, 1+channel.getPanning()) * value;
			}

			valueL = Math.max(-1, Math.min(1, valueL));
			valueR = Math.max(-1, Math.min(1, valueR));
			
			dataL[i] = (short)((valueL/2+0.5) * 0xffff - 0x8000);
			dataR[i] = (short)((valueR/2+0.5) * 0xffff - 0x8000);
			
			if (Thread.currentThread().isInterrupted() || sampleNow++ > sampleNext) tick();
		}
		isRendering = false;
		if (Thread.currentThread().isInterrupted()) tick();
	}
	
	private class State {
		protected int tick = 0;
		protected int row = 0;
		protected int pattern = 0;

		protected int patternTableIndex = 0;
		
		// TODO
		
		public State(Song song) {
			// TODO
		}
	}
}
