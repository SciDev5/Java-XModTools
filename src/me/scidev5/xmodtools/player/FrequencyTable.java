package me.scidev5.xmodtools.player;

public enum FrequencyTable {
	LINEAR((int trueNote, double fineTune) -> {
		return 7680 - (trueNote * 64) - (fineTune / 2.0);
	}, (double period) -> {
		return 8363 * Math.pow(2.0,((4608 - period) / 768.0));
	});
	// TODO Amiga
	

	private CalculatePeriod calculatePeriodFunction;
	private CalculateSampleRate calculateSampleRateFunction;
	private FrequencyTable(CalculatePeriod calculatePeriodFunction, CalculateSampleRate calculateSampleRateFunction) {
		this.calculatePeriodFunction = calculatePeriodFunction;
		this.calculateSampleRateFunction = calculateSampleRateFunction;
	}
	public double calculateSampleRate(int trueNote, byte fineTune) {
		return this.calculateSampleRateFunction.apply(this.calculatePeriod(trueNote, fineTune));
	}
	public double calculateSampleRate(int trueNote, byte fineTune, double pitchBend) {
		return this.calculateSampleRateFunction.apply(this.calculatePeriod(trueNote, fineTune, pitchBend));
	}
	public double calculatePeriod(int trueNote, byte fineTune) {
		return this.calculatePeriodFunction.apply(trueNote, fineTune);
	}
	public double calculatePeriod(int trueNote, byte fineTune, double pitchBend) {
		return this.calculatePeriodFunction.apply(trueNote, fineTune) + pitchBend;
	}

	@FunctionalInterface
	private interface CalculatePeriod {
		public double apply(int trueNote, double fineTune);
	}
	@FunctionalInterface
	private interface CalculateSampleRate {
		public double apply(double period);
	}
}
