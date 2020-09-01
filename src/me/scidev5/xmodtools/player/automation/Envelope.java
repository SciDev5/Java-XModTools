package me.scidev5.xmodtools.player.automation;

import me.scidev5.xmodtools.Constants;

public class Envelope {
	protected int numPoints = 0;
	protected int frame = 0;
	
	protected Point[] points;

	protected int sustain = -1;
	
	protected int loopStart = 0;
	protected int loopEnd = 0;
	protected boolean loop = false;
	
	protected int lastValue = 0;
	
	/**
	 * Add a point to the envelope.
	 * @param point The point to add.
	 */
	public void addPoint(Point point) {
		assert point != null : "New envelope point was null.";
		assert this.numPoints < Constants.MAX_ENV_POINTS : "Too many envelope points; max: "+Constants.MAX_ENV_POINTS;
		assert point.x > this.points[this.numPoints-1].x : "New envelope point is at or before the last point.";
		
		this.points[this.numPoints++] = point;
	}
	
	/**
	 * Set a point in the envelope by index.
	 * @param point The point to set.
	 * @param i The index to set at.
	 */
	public void setPoint(Point point, int i) {
		assert point != null : "New envelope point was null.";
		assert i >= 0 && i < this.numPoints : "Index is out of bounds.";
		if (i > 0)                assert this.points[i-1].x < point.x : "Envelope point is at or before the last point.";
		else                      assert point.x == 0 :                 "First envelope point not set to be at time 0";
		if (i < this.numPoints-1) assert this.points[i+1].x > point.x : "Envelope point is at or after the next point.";
		
		this.points[i] = point;
	}
	
	/**
	 * Get a point in the envelope by index.
	 * @param i The index of the point.
	 * @return The point at index i.
	 */
	public Point getPoint(int i) {
		assert i >= 0 && i < this.numPoints : "Index is out of bounds.";
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
		int i = 0;
		for (; i < this.numPoints - 1; i++) {
			if (points[i].x == this.frame) {
				if (this.loop && this.points[this.loopEnd].x == this.frame && this.points[this.loopStart].x < this.frame)
					this.frame = this.points[this.loopStart].x;
				else if (!shouldSustain || i != this.sustain)
					this.frame++;
				
				return points[i].y;
			}
			if (points[i].x < this.frame && points[i+1].x > this.frame)
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
	 * @param sustain
	 */
	public void setSustain(int sustain) {
		this.sustain = Math.max(0, Math.min(sustain, this.numPoints-1));
	}
	
	/**
	 * Disable sustain on the envelope. (sets sustain value to -1)
	 */
	public void clearSustain() {
		this.sustain = -1;
	}
	
	public class Point {
		public final int x;
		public final int y;
		public Point(int x, int y) {
			this.x = Math.min(0xffff, Math.max(0, x));
			this.y = Math.min(0x40, Math.max(0, y));
		}
	}
}
