package me.scidev5.xmodtools.player.automation;

import me.scidev5.xmodtools.Constants;

public class Envelope {
	protected int numPoints = 0;
	protected int frame = 0;
	
	protected Point[] points;

	protected int sustain = 0;
	private boolean sustainEnabled = false;
	
	protected int loopStart = 0;
	protected int loopEnd = 0;
	protected boolean loop = false;
	
	protected int lastValue = 0;
	
	public Envelope() {
		this.points = new Point[Constants.MAX_ENV_POINTS];
	}
	
	/**
	 * Add a point to the envelope.
	 * @param point The point to add.
	 */
	public void addPoint(Point point) {
		if (point == null) throw new IllegalArgumentException("New envelope point was null.");
		if (this.numPoints >= Constants.MAX_ENV_POINTS) new IllegalArgumentException("Too many envelope points; max: "+Constants.MAX_ENV_POINTS);
		if (point.x <= this.points[this.numPoints-1].x) new IllegalArgumentException("New envelope point is at or before the last point.");
		
		this.points[this.numPoints++] = point;
	}
	
	/**
	 * Set a point in the envelope by index.
	 * @param point The point to set.
	 * @param i The index to set at.
	 */
	public void setPoint(Point point, int i) {
		if (point == null) throw new IllegalArgumentException("New envelope point was null.");
		if (i < 0 || i >= this.numPoints) throw new IllegalArgumentException("Index is out of bounds.");
		if (i > 0)                if (this.points[i-1].x >= point.x) throw new IllegalArgumentException("Envelope point is at or before the last point.");
		else                      if (point.x != 0)                  throw new IllegalArgumentException("First envelope point not set to be at time 0");
		if (i < this.numPoints-1) if (this.points[i+1].x <= point.x) throw new IllegalArgumentException("Envelope point is at or after the next point.");
		
		this.points[i] = point;
	}
	
	/**
	 * Get a point in the envelope by index.
	 * @param i The index of the point.
	 * @return The point at index i.
	 */
	public Point getPoint(int i) {
		if (i < 0 || i >= this.numPoints) throw new IllegalArgumentException("Index is out of bounds.");
		return this.points[i];
	}
	
	/**
	 * Get the number of points in the envelope.
	 * @return The number of points.
	 */
	public int getNumPoints() {
		return this.numPoints;
	}
	
	/**
	 * Calculate the next frame of the envelope.
	 * @param shouldSustain Whether or not the envelope should stop at sustain points.
	 * @return The result.
	 */
	public int calculate(boolean shouldSustain) {
		if (this.numPoints == 0) {
			this.lastValue = 0;
			return this.lastValue;
		}
		
		int i = 0;
		for (; i < this.numPoints; i++) {
			if (points[i].x == this.frame) {
				if (this.loop && this.loopEnd == i) {
					this.frame = this.points[this.loopStart].x;
					i = this.loopStart;
				}
				if (!shouldSustain || !this.sustainEnabled || i != this.sustain)
					this.frame++;
				
				this.lastValue = this.points[i].y;
				return this.lastValue;
			}
			if (i == this.numPoints - 1)
				break;
			else if (points[i].x < this.frame && points[i+1].x > this.frame)
				break;
		}
		
		if (i == this.numPoints - 1)
			return this.points[this.numPoints-1].y;
		
		float interpolate = (this.frame - points[i].x)/(float)(points[i+1].x - points[i].x);
		this.lastValue = Math.round(points[i].y + interpolate * (points[i + 1].y - points[i].y));
		
		this.frame ++;
		
		return this.lastValue;
	}
	
	/**
	 * Get the result of the last frame of the envelope.
	 * @return The last frame's result. (Range: $0 - $40 (0 - 64)
	 */
	public int get() {
		return this.lastValue;
	}
	
	/**
	 * Set the loop parameters of the envelope.
	 * @param doLoop Whether or not to loop.
	 * @param startN Start point.
	 * @param endN End point.
	 */
	public void setLoop(boolean doLoop, int startN, int endN) {
		this.loop = doLoop;
		this.loopStart = Math.max(0, Math.min(startN, this.numPoints-1));
		this.loopEnd = Math.max(this.loopStart, Math.min(endN, this.numPoints-1));
	}
	
	/**
	 * Set the sustain point on the envelope. (Determines which point to stop at when holding a note)
	 * @param enabled If the sustain point is enabled.
	 * @param sustain The index of the point to hold at if sustain is enabled.
	 */
	public void setSustain(boolean enabled, int sustain) {
		this.sustainEnabled = enabled;
		this.sustain = Math.max(0, Math.min(sustain, this.numPoints-1));
	}

	/**
	 * Get the index for sustain on the envelope.
	 * @return The index of the point to hold at if sustain is enabled.
	 */
	public int getSustain() {
		return this.sustain;
	}
	
	/**
	 * Get if the sustain point is enabled.
	 * @return If sustain is enabled
	 */
	public boolean getSustainEnabled() {
		return this.sustainEnabled;
	}

	/**
	 * Get the index for loop start point.
	 * @return The index of the point to restart a loop from.
	 */
	public int getLoopStart() {
		return this.loopStart;
	}
	/**
	 * Get the index for loop end point.
	 * @return The index of the point to loop back from.
	 */
	public int getLoopEnd() {
		return this.loopEnd;
	}
	/**
	 * Get the index for loop start point.
	 * @return The index of the point to restart a loop from.
	 */
	public boolean getLoopEnabled() {
		return this.loop;
	}

	/**
	 * Reset the envelope to its starting position.
	 */
	public void retrigger() {
		this.frame = 0;
		if (this.numPoints > 0)
			this.lastValue = this.points[0].y;
	}
	
	/**
	 * Set the envelope's position.
	 * @param frame The position to set.
	 */
	public void setFrame(int frame) {
		this.frame = Math.max(0, frame);
	}
	
	
	public Envelope copy() {
		Envelope env = new Envelope();
		for (int i = 0; i < this.numPoints; i++) {
			env.addPoint(this.points[i]);
		}
		env.setLoop(this.loop, this.loopStart, this.loopEnd);
		env.setSustain(this.sustainEnabled, this.sustain);
		return env;
	}
	
	
	public static class Point {
		public final int x;
		public final int y;
		public Point(int x, int y) {
			this.x = Math.min(0xffff, Math.max(0, x));
			this.y = Math.min(0x40, Math.max(0, y));
		}
	}
}
