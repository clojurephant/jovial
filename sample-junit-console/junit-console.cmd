@echo off
cmd /c gradlew :installDist --quiet
cmd /c build\install\jupiter-sample-console\bin\jupiter-sample-console %*
