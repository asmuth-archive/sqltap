import AssemblyKeys._

name := "SQLTap"

organization := "com.paulasmuth"

version := "0.2.3"

mainClass in (Compile, run) := Some("com.paulasmuth.sqltap.SQLTap")

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "test")

scalaVersion := "2.9.1"

resolvers += "Couchbase Maven2 Repo" at "http://files.couchbase.com/maven2"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.12"

libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "7.2.2.v20101205"

libraryDependencies += "spy" % "spymemcached" % "2.8.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0.M1" % "test"

assemblySettings

jarName in assembly <<= (version) { v => "sqltap_" + v + ".jar" }

fork in run := true
