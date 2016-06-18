@echo off
cmd /c gradlew :jupiter-sample-console:installDist --quiet
cmd /c jupiter-sample-console\build\install\jupiter-sample-console\bin\jupiter-sample-console %*
