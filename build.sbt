name := "HttpClient"

organization := "com.github.mideo"

lazy val httpio = (project in file("."))
  .settings(
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-encoding",
      "utf8",
      "-feature",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:reflectiveCalls",
      "-Yrangepos"
    ),
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-streams" % "0.10.2.1",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.9",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.9.9",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.netty" % "netty-codec-http" % "4.1.39.Final",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.mockito" % "mockito-all" % "1.10.19" % Test,
      "com.github.tomakehurst" % "wiremock" % "2.23.2" % Test


    )
  )

fork in run := true

parallelExecution in httpio := false

testOptions in Test += Tests.Argument("-oDF")

resolvers ++= Seq(
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Sonatypes" at "https://oss.sonatype.org/content/repositories/releases",
  "Maven Repo" at "http://mvnrepository.com/maven2/"
)

scalaVersion := "2.12.6"

resolvers += Classpaths.typesafeReleases
resolvers += JCenterRepository



pomIncludeRepository := { _ => true }

publishMavenStyle := true

publishArtifact in Test := false

val oss_user = if (sys.env.keySet.contains("OSS_USERNAME")) sys.env("OSS_USERNAME") else ""
val oss_pass = if (sys.env.keySet.contains("OSS_PASSWORD")) sys.env("OSS_PASSWORD") else ""
val gpg_pass = if (sys.env.keySet.contains("GPG_PASSWORD")) sys.env("GPG_PASSWORD").toCharArray else Array.emptyCharArray

credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org", oss_user, oss_pass)

pgpPassphrase := Some(gpg_pass)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("https://github.com/MideO/HttpIO"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/MideO/HttpIO"),
    "scm:git@github.com/MideO/HttpIO"
  )
)

developers := List(
  Developer(
    id = "mideo",
    name = "Mide Ojikutu",
    email = "mide.ojikutu@gmail.com",
    url = url("https://github.com/MideO")
  )
)

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}
val tagOrHash = Def.setting {
  if (isSnapshot.value)
    sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else
    tagName.value
}


// Release
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseVersionBump := sbtrelease.Version.Bump.Next

releaseIgnoreUntrackedFiles := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)