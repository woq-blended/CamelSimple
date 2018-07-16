import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq
  
object Versions {
  val scalaVersion = "2.12.6"
} 

object Plugins {
  val scala = "net.alchim31.maven" % "scala-maven-plugin" % "3.3.2"
} 

val prjProperties : Map[String, String] = Map( 
  "java.version" -> "1.8",
  "scala.version" -> Versions.scalaVersion,
  "scala.binaryVersion" -> "2.12"
) 

val scalaCompilerConfig = Config(
  scalaVersion = Versions.scalaVersion,
  fork = "true",
  recompileMode = "incremental",
  useZincServer = "true",
  addJavacArgs = "-target|${java.version}|-source|${java.version}",
  addZincArgs = "-C-target|-C${java.version}|-C-source|-C${java.version}",
  args = Config(
    arg = "-deprecation",
    arg = "-feature",
    arg = "-Xlint",
    arg = "-Ywarn-nullary-override"
  ),
  jvmArgs = Config(
    jvmArg = "-Xms256m",
    jvmArg = "-Xmx512m",
    jvmArg = "-XX:MaxPermSize=128m"
  )
)

val scalaExecution_addSource: Execution = Execution(
  id = "scala-addSource",
  goals = Seq("add-source"),
  phase = "initialize",
  configuration = scalaCompilerConfig
)

val scalaExecution_compile: Execution = Execution(
  id = "scala-compile",
  goals = Seq("compile"),
  configuration = scalaCompilerConfig
)
val scalaExecution_testCompile: Execution = Execution(
  id = "scala-testCompile",
  goals = Seq("testCompile"),
  configuration = scalaCompilerConfig
)

val scalaMavenPlugin = Plugin(
  gav = Plugins.scala,
  executions = Seq(
    scalaExecution_addSource,
    scalaExecution_compile,
    scalaExecution_testCompile
  ),
  configuration = scalaCompilerConfig
)

val prjResources = Seq(
  Resource(
    filtering = true,
    directory = "src/main/resources"
  ),
  Resource(
    directory = "src/main/binaryResources"
  )
)

val prjTestResources = Seq(
  Resource(
    filtering = true,
    directory = "src/test/resources"
  ),
  Resource(
    directory = "src/test/binaryResources"
  )
)
	
Model(
  gav = "de.wayofquality.blended" % "CamelSimple" % "0.0.1-SNAPSHOT",
  properties = prjProperties,
  build = Build(
  	resources = prjResources, 
  	testResources = prjTestResources,
	plugins = Seq(
	  scalaMavenPlugin
	)
  )
)