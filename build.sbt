import AssemblyKeys._

name := "DPump"

organization := "com.paulasmuth"

version := "0.0.1"

scalaSource in Compile <<= baseDirectory(_ / "src")

mainClass in (Compile, run) := Some("com.paulasmuth.dpump.DPump")

scalaVersion := "2.9.1"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.12"

libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "7.2.2.v20101205"

libraryDependencies += "com.google.code.gson" % "gson" % "1.4"

assemblySettings

jarName in assembly <<= (version) { v => "dpump_" + v + ".jar" }
