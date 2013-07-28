import scopt.OptionParser
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import java.io.File


object ServerCompare extends App {

  Config.parser.parse(args, Config()) foreach { config =>

    val referenceFiles = getFiles(config.refHostname, config.path)
    val testFiles = getFiles(config.testHostname, config.path)

    val extraFiles = testFiles filterNot referenceFiles.contains
    val missingFiles = referenceFiles filterNot testFiles.contains
    val commonFiles = testFiles filter referenceFiles.contains
    val rsyncDifferentFiles = getDifferentFiles(config)
    val differentCommonFiles = rsyncDifferentFiles filter commonFiles.contains
    val identicalCommonFiles = commonFiles filterNot rsyncDifferentFiles.contains


    println(s"Files only on ${config.testHostname}:" )
    extraFiles.foreach { file => println(" * " + file) }
    println(s"Files only on ${config.refHostname}:" )
    missingFiles.foreach { file => println(" * " + file) }
    println(s"Differing files:")
    differentCommonFiles.foreach { file => println(" * " + file) }
    println(s"Files only on ${config.testHostname}: " + extraFiles.size);
    println(s"Files only on ${config.refHostname}: " + missingFiles.size)
    println("Common files: " + commonFiles.size)
    println("Different files: " + differentCommonFiles.size)
    println("Identical files: " + identicalCommonFiles.size)

    if(config.diff) {
      if(differentCommonFiles.size <= config.maxDiffFiles) {
        differentCommonFiles.par.foreach { file =>
          val tempRefFile = File.createTempFile(tmpFilePrefix(config.refHostname, file), ".tmp")
          tempRefFile.deleteOnExit()
          copyFileContents(config.refHostname, file, tempRefFile.getAbsolutePath)
          val tempTestFile = File.createTempFile(tmpFilePrefix(config.testHostname, file), ".tmp")
          tempTestFile.deleteOnExit()
          copyFileContents(config.testHostname, file, tempTestFile.getAbsolutePath)
          compareFiles(config, tempRefFile, tempTestFile)
        }
      } else println(s"${differentCommonFiles.size} changed files exceeds max diff files limit of {$config.maxDiffFiles}. Not showing diff.")
    }
  }

  def tmpFilePrefix(hostname: String, file: String) =
    sanitizeFilename(file) + "-" + sanitizeFilename(hostname)

  def sanitizeFilename(input: String): String =
    input.replaceAll("[^a-zA-Z0-9-_\\.]", "-")

  def compareFiles(config: Config, ref: File, test: File) = {
    val compareCommand = config.diffCommand + " " + ref.getAbsolutePath + " " + test.getAbsolutePath
    Process(compareCommand).!
  }

  def copyFileContents(hostname: String, remotePath: String, localPath: String): Unit = {
    val processLogger = new BufferingProcessLogger
    val command = Seq("scp", s"$hostname:$remotePath", localPath)
    val success = Process(command).!(processLogger) == 0
    if(!success) {
      println("Error on " + hostname + " for command '" + command + "'")
      processLogger.stdErr.foreach(println(_))
    }
  }

  def getFiles(hostname: String, path: String): Seq[String] = {
    val processLogger = new BufferingProcessLogger
    val command = Seq("ssh", hostname, "find", path)
    val success = Process(command).!(processLogger) == 0
    if(!success) {
      println("Error on " + hostname + " for command '" + command + "'")
      processLogger.stdErr.foreach(println(_))
    }
    processLogger.stdOut
  }

  def getDifferentFiles(config: Config): Seq[String] = {
    val processLogger = new BufferingProcessLogger
    val rsyncTarget = s"${config.testHostname}:${config.path}"
    val rsyncCommand = Seq("rsync", "--dry-run", "--recursive", "--itemize-changes", "--links", "--checksum", config.path, rsyncTarget)
    val success = Process(Seq("ssh", config.refHostname) ++ rsyncCommand).!(processLogger) == 0
    if(!success) {
      println("Error on " + config.refHostname + " for command '" + rsyncCommand + "'")
      processLogger.stdErr.foreach(println(_))
    }

    processLogger.stdOut.map { line =>
      val Array(changes, file) = line.split(" ", 2)

      file.split(" -> ", 2) match {
        case Array(linkSrc, linkTarget) => linkSrc
        case _ => file
      }
    } map { config.path + _ }
  }

}

class BufferingProcessLogger extends ProcessLogger {
  import scala.collection.mutable.ArrayBuffer
  private val stdOutBuffer = new ArrayBuffer[String]
  private val stdErrBuffer = new ArrayBuffer[String]

  override def out(s: => String) = stdOutBuffer.append(s)
  override def err(s: => String) = stdErrBuffer.append(s)
  override def buffer[T](f: => T): T = f

  lazy val stdOut = stdOutBuffer.toList
  lazy val stdErr = stdErrBuffer.toList
}

case class Config(
  testHostname: String = "",
  refHostname: String = "",
  path: String = "",
  diff: Boolean = true,
  maxDiffFiles: Int = 10,
  diffCommand: String = "opendiff")

object Config {
  def parser = new OptionParser[Config]("server-compare") {
    head("server-compare", "0.1")

    arg[String]("<reference host>") required () action { (v, c) =>
      c.copy(refHostname = v)
    } text ("Reference server")

    arg[String]("<test host>") required () action { (v, c) =>
      c.copy(testHostname = v)
    } text ("Server to test")

    arg[String]("<directory>") required () action { (v, c) =>
        c.copy(path = v)
    } text ("Directory on server, with trailing slash")

     opt[Boolean]('d', "diff") action { (v, c) =>
      c.copy(diff = v)
    } valueName("<bool>") text("Show differences")

    opt[Int]("max-diff-files") action { (v, c) =>
      c.copy(maxDiffFiles = v)
    } valueName("<int>") text("Max number of files to show differences for")

    opt[String]("diff-cmd") action { (v, c) =>
      c.copy(diffCommand = v)
    } valueName("<cmd>") text("Diff command. Should take two parameters for the two files to compare")

    help("help") text("prints this usage text")
  }
}