//> using scala 2.12.17
//> using lib 'com.johnsnowlabs.nlp:spark-nlp_2.12:5.1.0'
//>
import spark.implicits._
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.nlp.annotators._
import com.johnsnowlabs.nlp.annotators.audio.WhisperForCTC
import org.apache.spark.ml.Pipeline

val audioAssembler: AudioAssembler = new AudioAssembler()
  .setInputCol("audio_content")
  .setOutputCol("audio_assembler")

val speechToText: WhisperForCTC = WhisperForCTC
  .pretrained("asr_whisper_tiny", "xx") 
  .setInputCols("audio_assembler")
  .setOutputCol("text")

val pipeline: Pipeline = new Pipeline().setStages(Array(audioAssembler, speechToText))

val bufferedSource =
  scala.io.Source.fromFile("src/test/resources/audio/txt/librispeech_asr_0.txt")

val rawFloats = bufferedSource
  .getLines()
  .map(_.split(",").head.trim.toFloat)
  .toArray
bufferedSource.close

val processedAudioFloats = Seq(rawFloats).toDF("audio_content")

val result = pipeline.fit(processedAudioFloats).transform(processedAudioFloats)
result.select("text.result").show(truncate = false)
