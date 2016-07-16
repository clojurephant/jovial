@echo off
cmd /c gradlew :installDist --quiet
cmd /c build\install\sample-junit-console\bin\sample-junit-console %*
