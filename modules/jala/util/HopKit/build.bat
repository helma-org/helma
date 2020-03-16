::
:: Jala Project [http://opensvn.csie.org/traccgi/jala]
::
:: Copyright 2004 ORF Online und Teletext GmbH
::
:: Licensed under the Apache License, Version 2.0 (the ``License'');
:: you may not use this file except in compliance with the License.
:: You may obtain a copy of the License at
::
::    http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing, software
:: distributed under the License is distributed on an ``AS IS'' BASIS,
:: WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
:: See the License for the specific language governing permissions and
:: limitations under the License.
::
:: $Revision$
:: $LastChangedBy$
:: $LastChangedDate$
:: $HeadURL$
::


@echo off

rem This is build.bat for Ant 1.7

rem Define the path to the ANT binary directory
rem (traling slash is mandatory!)
rem set ANT_HOME=%PROGRAMFILES%\Apache Group\apache-ant-1.7.0\bin\

rem Set this to the directory of your Helma installation if necessary
set HELMA_HOME=""

rem ==========================================================================
rem No need to edit anything past here

rem store path of this script as BUILD_HOME
set BUILD_HOME=%~dp0

rem Set this to the directory of your Helma installation
if "%HELMA_HOME%"=="""" set HELMA_HOME="%BUILD_HOME%/../../../.."

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
set ANT_CMD_LINE_ARGS=%1
if ""%1""=="""" goto runAnt
shift
:setupArgs
if ""%1""=="""" goto runAnt
if ""%1""==""-noclasspath"" goto runAnt
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs

:runAnt

rem ---- if there is no build.xml in the working directory, use the library
rem ---- in this directory
if not exist ".\build.xml" (
   set ANT_CMD_LINE_ARGS=-file "%BUILD_HOME%lib.xml" %ANT_CMD_LINE_ARGS%
)

echo BUILD_HOME: %BUILD_HOME%

ant -Dant.home=. -Dbasedir=. -lib "%BUILD_HOME%\lib;%HELMA_HOME%\lib" -propertyfile "%CD%\build.properties" %ANT_CMD_LINE_ARGS%

:end
