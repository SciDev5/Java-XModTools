package me.scidev5.xmodtools.player;

public enum FrequencyTable {
	LINEAR((int trueNote, byte fineTune) -> {
		double period = 7680 - (trueNote * 64) - (fineTune / 2.0);
		return 8363 * Math.pow(2f,((4608 - period) / 768.0));
	});
	// TODO Amiga
	
	
	private Calculate calculateFunction;
	private FrequencyTable(Calculate calculateFunction) {
		this.calculateFunction = calculateFunction;
	}
	public double apply(int trueNote, byte fineTune) {
		return this.calculateFunction.apply(trueNote, fineTune);
	}
	
	@FunctionalInterface
	private interface Calculate {
		public double apply(int trueNote, byte fineTune);
	}
}
