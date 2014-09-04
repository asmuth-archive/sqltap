import AssemblyKeys._

name := "SQLTap"

organization := "com.paulasmuth"

version := "0.7.21"

mainClass in (Compile, run) := Some("com.paulasmuth.sqltap.SQLTap")

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaVersion := "2.9.1"

assemblySettings

jarName in assembly := "sqltap_0.7.21.jar"

fork in run := true
