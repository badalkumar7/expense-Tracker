@echo off
pushd "%~dp0"
".\tools\apache-maven-3.9.9\bin\mvn.cmd" "-Dmaven.repo.local=.m2" compile exec:java
popd
