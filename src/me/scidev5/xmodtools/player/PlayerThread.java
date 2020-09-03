package me.scidev5.xmodtools.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class PlayerThread extends Thread {
	
	private static final float SAMPLE_RATE = 44100f;
	private static final int FRAME_SIZE = 512;
	private static final int FRAME_BUFFER_SIZE = 2;
	private static final float FRAME_RATE = SAMPLE_RATE/FRAME_SIZE;
	
	private boolean running = true;
	private boolean ended = false;
	private AudioFormat audioFormat = null;
	
	private XMAudioController controller = null;
	
	public PlayerThread() {
		this.audioFormat = new AudioFormat(SAMPLE_RATE,16,2,true,false);
	}
	
	public void setController(XMAudioController controller) {
		this.controller = controller;
	}
	
	public AudioFormat getAudioFormat() {
		return this.audioFormat;
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
		
		final int volume = 1;
		
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
