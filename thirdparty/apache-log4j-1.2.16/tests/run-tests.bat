rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.
rem  Batch file for running tests on JDK 1.1
rem
SET CLASSPATH=\java\junit3.8.1\junit.jar;\java\crimson-1.1.3\crimson.jar;\java\jakarta-oro-2.0.8\jakarta-oro-2.0.8.jar;target\classes;..\..\target\classes;resources;%log4j.jar%
mkdir target
mkdir target\classes
cd src\java
javac -d ..\..\target\classes org\apache\log4j\util\SerializationTestHelper.java
javac -d ..\..\target\classes org\apache\log4j\spi\LoggingEventTest.java
javac -d ..\..\target\classes org\apache\log4j\LevelTest.java
javac -d ..\..\target\classes org\apache\log4j\FileAppenderTest.java
javac -d ..\..\target\classes org\apache\log4j\PriorityTest.java
javac -d ..\..\target\classes org\apache\log4j\CategoryTest.java
javac -d ..\..\target\classes org\apache\log4j\LogManagerTest.java
javac -d ..\..\target\classes org\apache\log4j\helpers\LogLogTest.java
javac -d ..\..\target\classes org\apache\log4j\LayoutTest.java
javac -d ..\..\target\classes org\apache\log4j\helpers\DateLayoutTest.java
javac -d ..\..\target\classes org\apache\log4j\TTCCLayoutTest.java
javac -d ..\..\target\classes org\apache\log4j\xml\XMLLayoutTest.java
javac -d ..\..\target\classes org\apache\log4j\HTMLLayoutTest.java
javac -d ..\..\target\classes org\apache\log4j\PatternLayoutTest.java
javac -d ..\..\target\classes org\apache\log4j\spi\ThrowableInformationTest.java
javac -d ..\..\target\classes org\apache\log4j\spi\LocationInfoTest.java
javac -d ..\..\target\classes org\apache\log4j\PropertyConfiguratorTest.java
javac -d ..\..\target\classes org\apache\log4j\CoreTestSuite.java
javac -d ..\..\target\classes org\apache\log4j\util\UnexpectedFormatException.java
javac -d ..\..\target\classes org\apache\log4j\util\Filter.java
javac -d ..\..\target\classes org\apache\log4j\util\Compare.java
javac -d ..\..\target\classes org\apache\log4j\util\ControlFilter.java
javac -d ..\..\target\classes org\apache\log4j\util\Transformer.java
javac -d ..\..\target\classes org\apache\log4j\util\LineNumberFilter.java
javac -d ..\..\target\classes org\apache\log4j\util\AbsoluteDateAndTimeFilter.java
javac -d ..\..\target\classes org\apache\log4j\MinimumTestCase.java
javac -d ..\..\target\classes org\apache\log4j\VectorAppender.java
javac -d ..\..\target\classes org\apache\log4j\LoggerTestCase.java
javac -d ..\..\target\classes org\apache\log4j\util\ISO8601Filter.java
javac -d ..\..\target\classes org\apache\log4j\util\SunReflectFilter.java
javac -d ..\..\target\classes org\apache\log4j\util\JunitTestRunnerFilter.java
javac -d ..\..\target\classes org\apache\log4j\xml\DOMTestCase.java
javac -d ..\..\target\classes org\apache\log4j\xml\XLevel.java
javac -d ..\..\target\classes org\apache\log4j\xml\CustomLevelTestCase.java
javac -d ..\..\target\classes org\apache\log4j\customLogger\XLogger.java
javac -d ..\..\target\classes org\apache\log4j\customLogger\XLoggerTestCase.java
javac -d ..\..\target\classes org\apache\log4j\defaultInit\TestCase1.java
javac -d ..\..\target\classes org\apache\log4j\defaultInit\TestCase3.java
javac -d ..\..\target\classes org\apache\log4j\defaultInit\TestCase4.java
javac -d ..\..\target\classes org\apache\log4j\util\XMLTimestampFilter.java
javac -d ..\..\target\classes org\apache\log4j\util\XMLLineAttributeFilter.java
javac -d ..\..\target\classes org\apache\log4j\xml\XMLLayoutTestCase.java
javac -d ..\..\target\classes org\apache\log4j\AsyncAppenderTestCase.java
javac -d ..\..\target\classes org\apache\log4j\helpers\OptionConverterTestCase.java
javac -d ..\..\target\classes org\apache\log4j\helpers\BoundedFIFOTestCase.java
javac -d ..\..\target\classes org\apache\log4j\helpers\CyclicBufferTestCase.java
javac -d ..\..\target\classes org\apache\log4j\or\ORTestCase.java
javac -d ..\..\target\classes org\apache\log4j\varia\LevelMatchFilterTestCase.java
javac -d ..\..\target\classes org\apache\log4j\helpers\PatternParserTestCase.java
javac -d ..\..\target\classes org\apache\log4j\util\AbsoluteTimeFilter.java
javac -d ..\..\target\classes org\apache\log4j\util\RelativeTimeFilter.java
javac -d ..\..\target\classes org\apache\log4j\PatternLayoutTestCase.java
javac -d ..\..\target\classes org\apache\log4j\MyPatternParser.java
javac -d ..\..\target\classes org\apache\log4j\MyPatternLayout.java
javac -d ..\..\target\classes org\apache\log4j\VectorErrorHandler.java
javac -d ..\..\target\classes org\apache\log4j\DRFATestCase.java
cd ..\..
mkdir output
java junit.textui.TestRunner org.apache.log4j.CoreTestSuite
java junit.textui.TestRunner org.apache.log4j.MinimumTestCase
java junit.textui.TestRunner org.apache.log4j.LoggerTestCase
java junit.textui.TestRunner org.apache.log4j.xml.DOMTestCase
java junit.textui.TestRunner org.apache.log4j.xml.CustomLevelTestCase
java junit.textui.TestRunner org.apache.log4j.customLogger.XLoggerTestCase
del target\classes\log4j.xml
del target\classes\log4j.properties
java junit.textui.TestRunner org.apache.log4j.defaultInit.TestCase1
copy input\xml\defaultInit.xml target\classes\log4j.xml
java junit.textui.TestRunner org.apache.log4j.defaultInit.TestCase2
del target\classes\log4j.xml
copy input\xml\defaultInit.xml target\classes\log4j.xml
java -Dlog4j.defaultInitOverride=true junit.textui.TestRunner org.apache.log4j.defaultInit.TestCase1
del target\classes\log4j.xml
copy input\defaultInit3.properties target\classes\log4j.properties
java junit.textui.TestRunner org.apache.log4j.defaultInit.TestCase3
del target\classes\log4j.properties
copy input\xml\defaultInit.xml target\classes\log4j.xml
copy input\defaultInit3.properties target\classes\log4j.properties
java junit.textui.TestRunner org.apache.log4j.defaultInit.TestCase4
del target\classes\log4j.xml
del target\classes\log4j.properties
java junit.textui.TestRunner org.apache.log4j.xml.XMLLayoutTestCase
java junit.textui.TestRunner org.apache.log4j.AsyncAppenderTestCase
java junit.textui.TestRunner org.apache.log4j.helpers.OptionConverterTestCase
java junit.textui.TestRunner org.apache.log4j.helpers.BoundedFIFOTestCase
java junit.textui.TestRunner org.apache.log4j.helpers.CyclicBufferTestCase
java junit.textui.TestRunner org.apache.log4j.or.ORTestCase
java junit.textui.TestRunner org.apache.log4j.varia.LevelMatchFilterTestCase
java junit.textui.TestRunner org.apache.log4j.helpers.PatternParserTestCase
java junit.textui.TestRunner org.apache.log4j.PatternLayoutTestCase
java junit.textui.TestRunner org.apache.log4j.DRFATestCase
