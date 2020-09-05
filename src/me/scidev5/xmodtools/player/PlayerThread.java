/*
 * XModTools  (A set of Java tools for using extended module song files.)
 * Copyright (C) 2020  SciDev5 (https://github.com/SciDev5)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * License file should be called "COPYING" which is in the root of the
 * master branch of theGitHub repositiory (https://github.com/SciDev5/Java-XModTools).
 */

package me.scidev5.xmodtools.player;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class PlayerThread extends Thread {
	
	private final float SAMPLE_RATE;
	private final int FRAME_SIZE;
	private final int FRAME_BUFFER_SIZE;
	private final float FRAME_RATE;
	
	private boolean running = true;
	private boolean ended = false;
	private AudioFormat audioFormat = null;
	
	private List<IAudioPostProcesser> postProcessers;
	
	private XMAudioController controller = null;
	
	/**
	 * Construct a new playerThread with custom audio parameters.
	 * @param sampleRate The rate at which samples are generated and played. (Default: 44100)
	 * @param frameSize The size of each frame. Smaller = little to no delay, inefficient; Larger = more delay, but more efficient. (Default: 512)
	 * @param frameBufferSize The amount of frames to buffer ahead so the sourceDataLine does not run empty. (Same tradeoffs as frameSize, default: 5)
	 */
	public PlayerThread(float sampleRate, int frameSize, int frameBufferSize) {
		this.SAMPLE_RATE = sampleRate;
		this.FRAME_SIZE = frameSize;
		this.FRAME_BUFFER_SIZE = frameBufferSize;
		this.FRAME_RATE = this.SAMPLE_RATE/this.FRAME_SIZE;
		
		this.audioFormat = new AudioFormat(SAMPLE_RATE,16,2,true,false);
		
		this.postProcessers = new ArrayList<>();
	}
	/**
	 * Construct a new playerThread with custom sample rate. (Frame size and rate are defaulted to 512 and 5 respectively).
	 * @param sampleRate The rate at which samples are generated and played. (Default: 44100)
	 */
	public PlayerThread(float sampleRate) {
		this(sampleRate, 512, 5);
	}
	/**
	 * Construct a new playerThread with generic audio parameters. 
	 * (Sample rate, frame size, and rate are defaulted to 44100, 512, and 5 respectively).
	 */
	public PlayerThread() {
		this(44100f);
	}
	
	/**
	 * Set the XMAudioController responsible for rendering audio
	 * @param controller
	 */
	public void setController(XMAudioController controller) {
		this.controller = controller;
	}
	
	/**
	 * Get the audio format 
	 * @return
	 */
	public AudioFormat getAudioFormat() {
		return this.audioFormat;
	}
	
	/**
	 * Get post processer list that can be modified to add new postProcessers.
	 * @return The postProcesser list.
	 */
	public List<IAudioPostProcesser> getPostProcesserList() {
		return this.postProcessers;
	}
	
	@Override
	public void run() {
		try {
			SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(this.audioFormat);
			byte[] buffer = new byte[4*FRAME_SIZE];
			sourceDataLine.open();
			sourceDataLine.start();
			final int fullSize = sourceDataLine.getBufferSize();
			outer:
			while (true) {
				while (fullSize - sourceDataLine.available() < FRAME_BUFFER_SIZE*4*FRAME_SIZE && this.running) {
					renderFrame(buffer);
					sourceDataLine.write(buffer, 0, 4*FRAME_SIZE);
				}
				while (this.running && fullSize-sourceDataLine.available() > FRAME_BUFFER_SIZE*4*FRAME_SIZE) {
					try {
						Thread.sleep((long)(1000/FRAME_RATE) + 1);
					} catch (InterruptedException e) {
						if (this.ended) break outer;
						if (!this.running) break;
					}
				}
				while (!this.running) {
					try {
						Thread.sleep(10000l);
					} catch (InterruptedException e) {
						if (this.ended) break outer;
						if (this.running) break;
					}
				}
				
			}
			sourceDataLine.close();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} finally {
			this.running = false;
			this.ended = true;
		}
	}

	private void transferFrame(short[] inDataL, short[] inDataR, byte[] buffer) {
		if (inDataL.length != FRAME_SIZE) throw new IllegalArgumentException();
		if (inDataR.length != FRAME_SIZE) throw new IllegalArgumentException();
		if (buffer.length != FRAME_SIZE*4) throw new IllegalArgumentException();
		
		final int volume = 0;
		
		for (int i = 0; i < FRAME_SIZE; i++) {
			buffer[4*i+0] = (byte) ((inDataL[i]>>(0+volume)) & 0xff);
			buffer[4*i+1] = (byte) ((inDataL[i]>>(8+volume)) & 0xff);
			
			buffer[4*i+2] = (byte) ((inDataR[i]>>(0+volume)) & 0xff);
			buffer[4*i+3] = (byte) ((inDataR[i]>>(8+volume)) & 0xff);
		}
	}
	
	private void renderFrame(byte[] buffer) {
		short[] dataL = new short[FRAME_SIZE];
		short[] dataR = new short[FRAME_SIZE];
		
		this.controller.render(dataL, dataR);
		
		for (IAudioPostProcesser processer : this.postProcessers)
			if (processer != null)
				processer.processData(dataL, dataR);
		
		transferFrame(dataL, dataR, buffer);
	}

	
	public float getSampleRate() {
		return SAMPLE_RATE;
	}
	
	// STATE MANAGEMENT
	
	public boolean isRunning() {
		return this.running && this.isAlive();
	}
	public void setRunning(boolean running) {
		if (this.isAlive()) {
			this.running = running;
			this.interrupt();
		}
	}
	public void end() {
		if (this.isAlive()) {
			this.ended = true;
			this.interrupt();
		}
	}
	public boolean isEnded() {
		return this.ended || !this.isAlive();
	}
}
