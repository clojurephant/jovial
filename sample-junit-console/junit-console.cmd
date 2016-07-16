@echo off
cmd /c gradlew :installDist --quiet
cmd /c build\install\jovial-sample-console\bin\jovial-sample-console %*
