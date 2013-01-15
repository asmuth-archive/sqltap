import AssemblyKeys._

name := "SQLTap"

organization := "com.paulasmuth"

version := "0.1.0"

scalaSource in Compile <<= baseDirectory(_ / "src")

mainClass in (Compile, run) := Some("com.paulasmuth.sqltap.SQLTap")

scalaVersion := "2.9.1"

resolvers += "Couchbase Maven2 Repo" at "http://files.couchbase.com/maven2"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.12"

libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "7.2.2.v20101205"

libraryDependencies += "spy" % "spymemcached" % "2.8.9"

assemblySettings

jarName in assembly <<= (version) { v => "sqltap_" + v + ".jar" }

fork in run := true
