package me.scidev5.xmodtools.player;

public enum SampleInterpolation {
	LINEAR((short sampleStart, short sampleEnd, double interpolate) -> {
		double start = ((sampleStart+0x8000)/(double)0xffff)*2-1;
		double end = ((sampleEnd+0x8000)/(double)0xffff)*2-1;
		return start + interpolate * (end - start);
	}),
	STEP((short sampleStart, short sampleEnd, double interpolate) -> {
		return ((sampleStart+0x8000)/(double)0xffff)*2-1;
	});
	// TODO add more interpolators
	
	
	private Calculate calculateFunction;
	private SampleInterpolation(Calculate calculateFunction) {
		this.calculateFunction = calculateFunction;
	}
	public double apply(short sampleStart, short sampleEnd, double interpolate) {
		return this.calculateFunction.apply(sampleStart, sampleEnd, interpolate);
	}
	public double apply(byte sampleStart, byte sampleEnd, double interpolate) {
		return this.calculateFunction.apply((short)(sampleStart<<8), (short)(sampleEnd<<8), interpolate);
	}
	
	@FunctionalInterface
	private interface Calculate {
		public double apply(short sampleStart, short sampleEnd, double interpolate);
	}
}
