import AssemblyKeys._

name := "SQLTap"

organization := "com.paulasmuth"

version := "0.0.7"

scalaSource in Compile <<= baseDirectory(_ / "src")

mainClass in (Compile, run) := Some("com.paulasmuth.sqltap.SQLTap")

scalaVersion := "2.9.1"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.12"

libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "7.2.2.v20101205"

assemblySettings

jarName in assembly <<= (version) { v => "sqltap_" + v + ".jar" }

fork in run := true
