import org.sonatype.maven.polyglot.scala.model._
import scala.collection.immutable.Seq
  
val sagumVersion = "9.9"  
val sagumLibdir = "src/main/libs" + sagumVersion

object Versions {
  val akka = "2.5.9"	
  val blended = "2.5.0-M8"	
  val camel = "2.17.3"	
  val scalaVersion = "2.12.6"
} 

implicit val scalaVersion = ScalaVersion(Versions.scalaVersion)

object Plugins {
  val scala = "net.alchim31.maven" % "scala-maven-plugin" % "3.3.2"
  val install = "org.apache.maven.plugins" % "maven-install-plugin" % "2.5.2"
  val exec = "org.codehaus.mojo" % "exec-maven-plugin" % "1.5.0"
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

def installSagLib(lib: String) : Execution = Execution(
  id = "install-" + lib,
  goals = Seq("install-file"),
  phase = "initialize",
  configuration = Config(
    file = "${project.basedir}/" + sagumLibdir + "/" + lib + ".jar",
    artifactId = lib, 
    groupId = "com.sagum",
    version = sagumVersion,
    packaging = "jar"
  )
)
	
Model(
  gav = "de.wayofquality.blended" % "CamelSimple" % "0.0.1-SNAPSHOT",
  properties = prjProperties,
  dependencies = Seq(
  	"de.wayofquality.blended" % "blended.jms.utils" % Versions.blended,
  	"org.apache.camel" % "camel-core" % Versions.camel,
  	"org.apache.camel" % "camel-jms" % Versions.camel,

  	"com.typesafe.akka" %% "akka-actor" % Versions.akka,
    "com.typesafe.akka" %% "akka-stream" % Versions.akka,
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,

    "com.sagum" % "nAdminAPI" % sagumVersion, 
    "com.sagum" % "nClient" % sagumVersion, 
    "com.sagum" % "nJMS" % sagumVersion, 
    
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-core" % "1.2.3",
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  ),

  build = Build(
  	resources = prjResources, 
  	testResources = prjTestResources,
	  plugins = Seq(
	    scalaMavenPlugin,
      Plugin(
        gav = Plugins.install,
        executions = Seq(
          installSagLib("nAdminAPI"),
          installSagLib("nClient"),
          installSagLib("nJMS")
        )
      ),
      Plugin(
        gav = Plugins.exec,
        executions = Seq(
          // To run CamelSimple, exec: mvn exec:java@CamelSimple
          Execution(
            id = "CamelSimple",
            goals = Seq("java"),
            phase = "none",
            configuration = Config(
              mainClass = "blended.camelsimple.CamelSimple"
            )
          )
        )
      )
	  )
  )
)