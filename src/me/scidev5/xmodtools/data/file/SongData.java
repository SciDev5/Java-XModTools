package me.scidev5.xmodtools.data.file;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.data.Instrument;
import me.scidev5.xmodtools.data.LoopType;
import me.scidev5.xmodtools.data.Pattern;
import me.scidev5.xmodtools.data.Pattern.PatternData;
import me.scidev5.xmodtools.data.Sample;
import me.scidev5.xmodtools.data.Song;

public class SongData {
	
	public static Song parseBytes(ByteBuffer dataIn) throws Exception {
		ByteBuffer data = dataIn;
		data.order(ByteOrder.LITTLE_ENDIAN);
		
		if (!DataUtils.getString(data, 17).equals("Extended Module: ")) {
			throw new Exception("XM ID Text was not 'Extended Module: '");
		}
		
		// ---- // HEADER // ---- //
		
		String moduleName = DataUtils.getString(dataIn, 20);
		if (dataIn.get() != 0x1a) throw new Exception("byte at position 37 was not equal to $1a");
		String trackerName = DataUtils.getString(dataIn, 20);
		if (dataIn.getShort() != 0x0104) throw new Exception("XM Version not equal to $0104");
		
		final int songHeaderLength = dataIn.getInt(); // Header length (dealt with elsewhere).

		Song song = new Song();
		song.setModuleName(moduleName);
		song.setTrackerName(trackerName);
		song.setSongLength(dataIn.getShort());
		song.setSongLoopPos(dataIn.getShort());
		song.setNumChannels(dataIn.getShort());
		short numPatterns = dataIn.getShort();
		short numInstruments = dataIn.getShort();
		short flags = dataIn.getShort(); // TODO select frequency tables
		
		song.setDefaultTempo(dataIn.getShort(),dataIn.getShort());
		
		data = sliceBuffer(data,60+songHeaderLength);

		// ---- // PATTERNS // ---- //
		
		for (int i = 0; i < numPatterns; i++) {
			data = parsePattern(data,song);
		}
		
		// ---- // Instruments // ---- //
		
		for (int i = 0; i < numInstruments; i++) {
			data = parseInstrument(data,song);
		}
		
		// TODO next part
		
		return song;
	}
	private static ByteBuffer sliceBuffer(ByteBuffer data, int pos) {
		data.position(pos);
		return data.slice().order(data.order());
	}
	
	private static ByteBuffer parsePattern(ByteBuffer data, Song song) throws Exception {
		final int headerLen = data.getInt();
		
		if (data.get() != 0) throw new Exception("Packing type was not 0.");
		
		short nRows = data.getShort();
		if (nRows < 0 || nRows > 256) throw new Exception("nRows was not between 1 and 256");
		
		final int packedPatternDataLen = data.getShort();
		data.position(headerLen);
		
		Pattern pattern = new Pattern(song, nRows);
		song.addPattern(pattern);
		
		int i = 0;
		while (data.position() < headerLen + packedPatternDataLen) {
			byte dbyt = data.get();
			byte note = 0; byte instrument = 0; byte volume = 0; byte effectType = 0; byte effectData = 0; 
			if ((dbyt & 0x80) != 0) {
				if ((dbyt & 0x01) != 0) note = data.get();
				if ((dbyt & 0x02) != 0) instrument = data.get();
				if ((dbyt & 0x04) != 0) volume = data.get();
				if ((dbyt & 0x08) != 0) effectType = data.get();
				if ((dbyt & 0x10) != 0) effectData = data.get();
			} else {
				note = dbyt;
				instrument = data.get();
				volume = data.get();
				effectType = data.get();
				effectData = data.get();
			}
			
			if (i >= nRows * song.getNumChannels()) throw new Exception("Too much row data, data does not fit.");
			
			pattern.setData(new PatternData(note, instrument, volume, effectType, effectData), i / song.getNumChannels(), i % song.getNumChannels());
			i++;
		}
		if (i < nRows * song.getNumChannels()) throw new Exception("Too little row data, data does not fit.");
		
		return sliceBuffer(data, headerLen + packedPatternDataLen);
	}
	private static ByteBuffer parseInstrument(ByteBuffer dataIn, Song song) {
		ByteBuffer data = dataIn;
		
		final int headerLen = data.getInt();
		
		String instrumentName = DataUtils.getString(data, 22);
		data.get(); // Instrument type (not used).
		
		System.out.println(instrumentName);
		
		int numSamples = data.getShort() & 0xffff;
		
		if (numSamples > 0) {
			Instrument instrument = new Instrument().setName(instrumentName);
			
			final int sampleHeaderLen = data.getInt();
			
			byte[] noteSampleMap = new byte[Constants.NOTE_COUNT];
			data.get(noteSampleMap);
			instrument.setSampleNoteMap(noteSampleMap);

			short[] volumeEnvPoints = new short[Constants.MAX_ENV_POINTS * 2];
			short[] panningEnvPoints = new short[Constants.MAX_ENV_POINTS * 2];
			ShortBuffer shortBuffer = data.asShortBuffer();
			shortBuffer.get(volumeEnvPoints);
			shortBuffer.get(panningEnvPoints);
			
			data.position(data.position() + Constants.MAX_ENV_POINTS * 4 * 2);

			int numVolumeEnvPoints = data.get() & 0xff;
			int numPanningEnvPoints = data.get() & 0xff;

			int volumeSustain = data.get() & 0xff;
			int volumeLoopStart = data.get() & 0xff;
			int volumeLoopEnd = data.get() & 0xff;
			
			int panningSustain = data.get() & 0xff;
			int panningLoopStart = data.get() & 0xff;
			int panningLoopEnd = data.get() & 0xff;

			byte volumeEnvType = data.get();
			byte panningEnvType = data.get();
			
			// TODO assemble and add envelopes;
			
			int autoVibratoType = data.get() & 0xff;
			int autoVibratoSweep = data.get() & 0xff;
			int autoVibratoDepth = data.get() & 0xff;
			int autoVibratoRate = data.get() & 0xff;
			
			// TODO assemble and add autovibrato;
			
			short volumeFadeout = data.getShort();
			instrument.setFadeout(volumeFadeout);
			
			data.getShort(); // Reserved
			
			song.addInstrument(instrument);
			
			data = sliceBuffer(data, headerLen);
			
			// ---- // SAMPLES // ---- //
			
			long[] lengths = new long[numSamples];
			Sample[] samples = new Sample[numSamples];
			
			for (int i = 0; i < numSamples; i++) {

				int length = data.getInt();
				
				lengths[i] = Integer.toUnsignedLong(length);
				
				long loopStart = Integer.toUnsignedLong(data.getInt());
				long loopLength = Integer.toUnsignedLong(data.getInt());

				byte volume = data.get();
				byte fineTune = data.get();
				byte type = data.get();
				LoopType loopType = (type & 0x01) != 0 ? ((type & 0x02) != 0 ? LoopType.PING_PONG : LoopType.FORWARD) : LoopType.NONE;
				boolean is16Bit = (type & 0x10) != 0;
				byte panning = data.get();
				byte relativeNote = data.get();
				
				data.get(); // Reserved
				
				String name = DataUtils.getString(data, 22);
				
				Sample sample;
				if (is16Bit) sample = new Sample(new short[length < 0 ? Integer.MAX_VALUE : length]);
				else         sample = new Sample(new byte[length < 0 ? Integer.MAX_VALUE : length]);
				
				sample.setName(name);
				sample.setDefaultVolume(volume);
				sample.setDefaultPanning(panning);
				sample.setRelativeNote(relativeNote);
				sample.setDefaultFineTune(fineTune);
				sample.setLoop(loopType, loopStart, loopLength);
				
				instrument.addSample(sample);
				samples[i] = sample;
				
				data = sliceBuffer(data, sampleHeaderLen);
			}

			for (int i = 0; i < numSamples; i++) {
				
				Sample sample = samples[i];
				
				if (sample.get16Bit()) {
					short current = 0;
					short[] sampleData = sample.getData16();
					for (int j = 0; j < sampleData.length; j++) {
						current += data.getShort();
						sampleData[j] = current;
					}
				} else {
					byte current = 0;
					byte[] sampleData = sample.getData8();
					for (int j = 0; j < sampleData.length; j++) {
						current += data.get();
						sampleData[j] = current;
					}
				}
				
				data = sliceBuffer(data, (int)lengths[i]);
			}
			
			return data; // TODO change to be sure it jumps to next pos
		} else {
			song.addInstrument(new Instrument().setName(instrumentName));
			return sliceBuffer(data,headerLen);
		}
	}
	
	public static Song loadFromFile(File file) throws Exception {
		FileInputStream fileIn = new FileInputStream(file);
		byte[] bytes = new byte[fileIn.available()];
		fileIn.read(bytes);
		fileIn.close();
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return parseBytes(bb);
	}
	
	/*
	 * 
  
  ////// INSTRUMENTS //////
  
  for (var i = 0; i < numInstruments; i++) {
    var instrumentHeaderLen = Uint8ArrayUtils.getDWord(data,currentIndex);
    var name = Uint8ArrayUtils.toString(data.slice(currentIndex+4,currentIndex+26));
    var numSamples = Uint8ArrayUtils.getWord(data,currentIndex+27);
    
    var instrument = new Instrument(name);
    
    song.addInstrument(instrument);
    
    if (!isFinite(numSamples) || numSamples == 0) {
      currentIndex += instrumentHeaderLen;
      continue;
    }
    
    var sampleHeaderSize = Uint8ArrayUtils.getDWord(data,currentIndex+29);
    var noteSampleMap = data.slice(currentIndex+33,currentIndex+129);
    instrument.setSampleMap(noteSampleMap);
    
    var volumeEnvType = data[currentIndex+233];
    if (volumeEnvType & 0x01) {
      var volumeEnv = new Envelope();
      var numVolumeEnvPoints = data[currentIndex+225];
      for (var j = 0; j < numVolumeEnvPoints && j < 12; j++) {
        var x = Uint8ArrayUtils.getWord(data,currentIndex+129+j*4);
        var y = Uint8ArrayUtils.getWord(data,currentIndex+129+j*4+2);
        volumeEnv.addPoint(new EnvelopePoint(x,y));
      }
      if (volumeEnvType & 0x02) {
        var sustain = data[currentIndex+227]
        volumeEnv.setSustain(sustain);
      }
      if (volumeEnvType & 0x04) {
        var loopStart = data[currentIndex+228];
        var loopEnd = data[currentIndex+229];
        volumeEnv.setLoop(loopStart,loopEnd);
      }
      instrument.setVolumeEnv(volumeEnv);
    }
    
    var panningEnvType = data[currentIndex+234];
    if (panningEnvType & 0x01) {
      var panningEnv = new Envelope();
      var numPanningEnvPoints = data[currentIndex+226];
      for (var j = 0; j < numPanningEnvPoints && j < 12; j++) {
        var x = Uint8ArrayUtils.getWord(data,currentIndex+177+j*4);
        var y = Uint8ArrayUtils.getWord(data,currentIndex+177+j*4+2);
        panningEnv.addPoint(new EnvelopePoint(x,y));
      }
      if (panningEnvType & 0x02) {
        var sustain = data[currentIndex+230]
        panningEnv.setSustain(sustain);
      }
      if (panningEnvType & 0x04) {
        var loopStart = data[currentIndex+231];
        var loopEnd = data[currentIndex+232];
        panningEnv.setLoop(loopStart,loopEnd);
      }
      instrument.setPanningEnv(panningEnv);
    }
    
    var vibratoType = data[currentIndex+235];
    var vibratoSweep = data[currentIndex+236];
    var vibratoDepth = data[currentIndex+237];
    var vibratoRate = data[currentIndex+238];
    
    var volumeFadeout = Uint8ArrayUtils.getWord(data,currentIndex+239);
    instrument.setFadeout(volumeFadeout);
    
    var reserved = Uint8ArrayUtils.getWord(data,currentIndex+241);
    
    currentIndex += instrumentHeaderLen;
    
    ////// SAMPLES //////
    
    for (var j = 0; j < numSamples; j++) {      
      var sampleLength = Uint8ArrayUtils.getDWord(data,currentIndex);
      
      var loopStart = Uint8ArrayUtils.getDWord(data,currentIndex+4);
      var loopLength = Uint8ArrayUtils.getDWord(data,currentIndex+8);
      
      var volume = data[currentIndex+12];
      var panning = data[currentIndex+15];
      Assert.inRange(volume,0,0x40,"volume");
      
      var fineTune = Uint8ArrayUtils.getSignedByte(data,currentIndex+13);
      var relativeNote = Uint8ArrayUtils.getSignedByte(data,currentIndex+16);
      //Assert.inRangeMxEx(fineTune,-16,16,"fineTune");
      
      var type = data[currentIndex+14];
      var loopType = loopLength == 0 ? 0 : type & 0x03;
      var is16Bit = (type & 0x10) != 0;
      
      var reserved = data[currentIndex+17];
      
      var name = Uint8ArrayUtils.toString(data.slice(currentIndex+18,currentIndex+40));
      
      var sample = new Sample(sampleLength*(is16Bit?0.5:1), name, volume, panning, fineTune, relativeNote, is16Bit);
      if (loopType != 0)
        sample.setLoop(loopType, loopStart*(is16Bit?0.5:1), loopLength*(is16Bit?0.5:1));
      
      instrument.addSample(sample);
      
      
      currentIndex += sampleHeaderSize;
    }
    
    for (var j = 0; j < numSamples; j++) {
      var sample = instrument.getSample(j);
      if (sample.is16Bit) {
        var sampleData = new Int16Array(sample.length);
        var value = 0;
        for (var byteI = 0; byteI < sample.length; byteI++) {
          value += Uint8ArrayUtils.getSignedWord(data, currentIndex+byteI*2);
          sampleData[byteI] = value;
        }
        sample.set(sampleData);
      } else {
        var deltaCoded = new Int8Array(data.slice(currentIndex,currentIndex + sample.length));
        var sampleData = new Int8Array(sample.length);
        var value = 0;
        for (var byteI = 0; byteI < sample.length; byteI++) {
          value += deltaCoded[byteI];
          sampleData[byteI] = value;
        }
        sample.set(sampleData);
      }
      currentIndex += sample.length*(sample.is16Bit?2:1);
    }
    
  }
    
  return song;
	 */
}
