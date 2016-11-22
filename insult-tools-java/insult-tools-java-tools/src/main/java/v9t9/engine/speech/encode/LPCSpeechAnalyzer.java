package v9t9.engine.speech.encode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFormat;

import uk.co.labbookpages.WavFile;
import uk.co.labbookpages.WavFileException;
import v9t9.common.speech.ILPCParameters;

public class LPCSpeechAnalyzer {

  private static final LPCConverter LPC_CONVERTER = new LPCConverter();

  // nominal speech reproduction rate
  public static final int PLAYBACK_HZ = 8000;

  // nominal framerate of 25 ms
  public static final int FRAMES_PER_SECOND = 40;

  // playback speed and FPS can be configured if required
  private final int playbackHz;
  private final int framesPerSecond;

  // suppliers allow the engine and filter to be configured externally if required
  private final Function<LPCEncoderParams, ILPCEngine> engineSupplier;
  private final Function<LPCEncoderParams, ILPCFilter> filterSupplier;

  public LPCSpeechAnalyzer() {
    this(PLAYBACK_HZ, FRAMES_PER_SECOND);
  }

  public LPCSpeechAnalyzer(int playbackHz, int framesPerSecond) {
    this(playbackHz, framesPerSecond, LPCSpeechAnalyzer::defaultEngine, LPCSpeechAnalyzer::defaultFilter);
  }

  public LPCSpeechAnalyzer(int playbackHz, int framesPerSecond, Function<LPCEncoderParams, ILPCEngine> engineSupplier,
          Function<LPCEncoderParams, ILPCFilter> filterSupplier) {
    this.playbackHz = playbackHz;
    this.framesPerSecond = framesPerSecond;
    this.engineSupplier = engineSupplier;
    this.filterSupplier = filterSupplier;
  }

  public List<ILPCParameters> analyze(Path wavFile) throws IOException, WavFileException {
    return analyzeFrames(wavFile).stream().map(LPC_CONVERTER::apply).collect(Collectors.toList());
  }

  private List<LPCAnalysisFrame> analyzeFrames(Path wavFile) throws IOException, WavFileException {
    WavFile wf = WavFile.openWavFile(wavFile.toFile());
    try {
      return analyzeWF(wf);
    } finally {
      wf.close();
    }
  }

  private List<LPCAnalysisFrame> analyzeWF(WavFile wf) throws IOException, WavFileException {
    // analyse WavFile, create engine and filter
    AudioFormat format = new AudioFormat(wf.getSampleRate(), wf.getValidBits(), wf.getNumChannels(), true, false);
    LPCEncoderParams params = new LPCEncoderParams((int) format.getFrameRate(), playbackHz, framesPerSecond, 10);
    ILPCEngine engine = engineSupplier.apply(params);
    ILPCFilter filter = filterSupplier.apply(params);

    // create our buffer variables
    int frames = (int) format.getFrameRate() / framesPerSecond;
    double[][] buffer = new double[format.getChannels()][frames];
    float[] content = new float[frames];
    int len;
    int nc = format.getChannels();

    // do the analysis
    List<LPCAnalysisFrame> analysisFrames = new ArrayList<>();
    while ((len = wf.readFrames(buffer, frames)) > 0) {
      for (int i = 0; i < len; i += nc) {
        content[i] = (float) buffer[0][i];
      }
      analysisFrames.add(encodeFrame(engine, filter, content));
    }
    return analysisFrames;
  }

  private static LPCAnalysisFrame encodeFrame(ILPCEngine engine, ILPCFilter filter, float[] content) {
    int len = content.length;
    if (filter != null) {
      filter.filter(content, 0, len, content, engine.getY());
    }
    return engine.analyze(content, 0, len);
  }

  private static ILPCFilter defaultFilter(LPCEncoderParams params) {
    return new LowPassLPCFilter(params, new SimpleLPCFilter(params));
  }

  private static ILPCEngine defaultEngine(LPCEncoderParams params) {
    return new OpenLPCEngine(params);
  }

}
