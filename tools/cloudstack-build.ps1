#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

<#
.SYNOPSIS
Runs a Windows-safe Apache CloudStack Maven build.

.DESCRIPTION
Uses the local Codex Java/Maven toolchain when present, verifies that Java 17+
is available, keeps Bash scripts on LF line endings, and promotes a server
compile request to package so cloud-api test classes are produced for the
cloud-server dependency graph.

.EXAMPLE
powershell -NoProfile -ExecutionPolicy Bypass -File tools\cloudstack-build.ps1 -Modules server -Phase compile

.EXAMPLE
powershell -NoProfile -ExecutionPolicy Bypass -File tools\cloudstack-build.ps1 -Modules server -Test VmwareCbtMigrationCutoverPolicyTest
#>

[CmdletBinding()]
param(
    [string[]] $Modules = @("server"),

    [ValidateSet("validate", "compile", "test-compile", "test", "package", "verify", "install")]
    [string] $Phase = "package",

    [string] $Test,

    [switch] $RunTests,

    [switch] $NoAutoPackage,

    [string] $JavaHome = "$env:USERPROFILE\.codex\toolchains\jdk-17",

    [string] $MavenHome = "$env:USERPROFILE\.codex\toolchains\apache-maven-3.9.9"
)

$ErrorActionPreference = "Stop"

function Get-ExistingFile([string[]] $Candidates) {
    foreach ($candidate in $Candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }
    return $null
}

function Join-ModuleList([string[]] $ModuleValues) {
    $values = @()
    foreach ($moduleValue in $ModuleValues) {
        if ([string]::IsNullOrWhiteSpace($moduleValue)) {
            continue
        }
        $values += ($moduleValue -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
    return ($values -join ",")
}

function Convert-FileToLf([string] $Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    $content = [System.IO.File]::ReadAllText($Path, $utf8NoBom)
    $normalized = $content.Replace("`r`n", "`n")
    if ($normalized -ne $content) {
        [System.IO.File]::WriteAllText($Path, $normalized, $utf8NoBom)
        Write-Host "Normalized LF line endings for $Path"
    }
}

function Get-JavaMajorVersion([string] $JavaExecutable) {
    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = $JavaExecutable
    $processInfo.Arguments = "-version"
    $processInfo.RedirectStandardError = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.UseShellExecute = $false

    $process = [System.Diagnostics.Process]::Start($processInfo)
    $standardError = $process.StandardError.ReadToEnd()
    $standardOutput = $process.StandardOutput.ReadToEnd()
    $process.WaitForExit()

    $combinedOutput = "$standardError`n$standardOutput"
    $versionLine = $combinedOutput -split "`r?`n" |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -First 1

    if ($versionLine -notmatch 'version "([^"]+)"') {
        throw "Could not determine Java version from: $versionLine"
    }

    $versionParts = $Matches[1].Split(".")
    if ($versionParts[0] -eq "1" -and $versionParts.Length -gt 1) {
        return [int] $versionParts[1]
    }
    return [int] $versionParts[0]
}

function Add-PathPrefix([string] $PathValue) {
    if (-not [string]::IsNullOrWhiteSpace($PathValue)) {
        $env:Path = "$PathValue;$env:Path"
    }
}

$moduleList = Join-ModuleList $Modules
if ([string]::IsNullOrWhiteSpace($moduleList)) {
    throw "At least one Maven module must be provided."
}

$requestedPhase = $Phase
$serverModules = @("server", ":cloud-server", "cloud-server")
if (-not $NoAutoPackage -and $Phase -eq "compile") {
    $selectedModules = $moduleList -split ","
    foreach ($serverModule in $serverModules) {
        if ($selectedModules -contains $serverModule) {
            $Phase = "package"
            Write-Host "Using Maven phase 'package' instead of 'compile' because cloud-server depends on cloud-api:tests."
            break
        }
    }
}

if ($env:OS -eq "Windows_NT") {
    Convert-FileToLf (Join-Path (Get-Location) "engine\schema\templateConfig.sh")
}

$javaExe = Get-ExistingFile @((Join-Path $JavaHome "bin\java.exe"))
$mvnCmd = Get-ExistingFile @((Join-Path $MavenHome "bin\mvn.cmd"))

if ($javaExe -ne $null) {
    $env:JAVA_HOME = $JavaHome
    Add-PathPrefix (Join-Path $JavaHome "bin")
} else {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand -eq $null) {
        throw "Java was not found. Install Java 17+ or pass -JavaHome."
    }
    $javaExe = $javaCommand.Source
}

$javaMajorVersion = Get-JavaMajorVersion $javaExe
if ($javaMajorVersion -lt 17) {
    throw "Java 17+ is required for this CloudStack build. Found Java major version $javaMajorVersion at $javaExe."
}

if ($mvnCmd -ne $null) {
    $env:MAVEN_HOME = $MavenHome
    Add-PathPrefix (Join-Path $MavenHome "bin")
} else {
    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCommand -eq $null) {
        throw "Maven was not found. Install Maven or pass -MavenHome."
    }
    $mvnCmd = $mvnCommand.Source
}

$mavenArgs = @("-pl", $moduleList, "-am")

if ([string]::IsNullOrWhiteSpace($Test)) {
    if (-not $RunTests) {
        $mavenArgs += @("-DskipTests", "-DskipITs")
    }
} else {
    $mavenArgs += @("-Dtest=$Test", "-DfailIfNoTests=false", "-Dsurefire.failIfNoSpecifiedTests=false", "-DskipITs")
}

$mavenArgs += @("-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", $Phase)

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"
Write-Host "Java executable: $javaExe"
Write-Host "Java major version: $javaMajorVersion"
Write-Host "Requested phase: $requestedPhase"
Write-Host "Effective phase: $Phase"
Write-Host "Running: mvn $($mavenArgs -join ' ')"

& $mvnCmd @mavenArgs
exit $LASTEXITCODE
