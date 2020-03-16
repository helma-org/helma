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

set HOPKIT=%~dp0

set JAVA=java
set JSJAR="%HOPKIT%\lib\js.jar"
set JSTOOLS=org.mozilla.javascript.tools

goto %1


rem -- S H E L L  --
:shell

if "%2"=="" (
   %JAVA% -cp "%JSJAR%" "%JSTOOLS%.shell.Main"
) else (
   %JAVA% -cp "%JSJAR%" "%JSTOOLS%.shell.Main" "%2"
)

goto end


rem -- T E S T --
:test


rem -- C O M P I L E --
:compile

%JAVA% -cp "%JSJAR%" "%JSTOOLS%.jsc.Main" -nosource "%2"

goto end


rem -- L I N T --
:lint

%JAVA% -cp "%JSJAR%" "%JSTOOLS%.shell.Main jslint.js" "%2"

goto end


rem -- D O C S --
:docs

if "%3"=="" (
   set DOCDIR=docs
) else (
   set DOCDIR=%3
)

"%HOPKIT%\JSDoc\jsdoc.pl" -q -r -d "%DOCDIR%" --template-dir "%HOPKIT%/JSDoc/templates" --package-naming --globals-name Global --project-name "%2" --project-summary "./.jsdoc/summary.html" .

goto end


rem -- M I N I F Y --
:minify

"%HOPKIT%\jsmin" "%2"


rem -- P A C K --
:pack


rem -- H E L P --
:help

echo Currently the following build targets are supported:
echo     docs     - Create JSDoc API documentation.
echo     help     - Output this information.
echo     compile  - Compile a JavaScript file as Java class.
echo     lint     - Apply JavaScript Lint to a file.
echo     minify   - Minify JavaScript source files.
rem echo     pack     - Pack and obsfucate JavaScript source files.
echo     shell    - Start Rhino's interactive JavaScript shell.
echo     test     - Apply unit tests to JavaScript code.


rem -- E N D --
:end
