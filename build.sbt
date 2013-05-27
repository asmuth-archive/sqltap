import AssemblyKeys._

name := "SQLTap"

organization := "com.paulasmuth"

version := "0.2.5"

mainClass in (Compile, run) := Some("com.paulasmuth.sqltap.SQLTap")

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "test")

scalaVersion := "2.9.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M1" % "test"

assemblySettings

jarName in assembly <<= (version) { v => "sqltap_" + v + ".jar" }

fork in run := true
