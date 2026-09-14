# Dot-source this script to configure the current PowerShell session.
$ProjectRoot = $PSScriptRoot
$NodeHome = Join-Path $ProjectRoot '.local\tools\node-v16.20.2-win-x64'
$MavenHome = Join-Path $ProjectRoot '.local\tools\apache-maven-3.9.11'
$JavaHome = 'D:\java\openjdk-8u312-b07'

foreach ($ToolPath in @($NodeHome, $MavenHome, $JavaHome)) {
    if (-not (Test-Path $ToolPath)) {
        throw "Required development tool was not found: $ToolPath"
    }
}

$env:JAVA_HOME = $JavaHome
$env:MAVEN_HOME = $MavenHome
$MavenRepository = 'E:/ia-dev/maven-repository'
$env:MAVEN_OPTS = "-Dmaven.repo.local=$MavenRepository $env:MAVEN_OPTS".Trim()
$env:Path = "$NodeHome;$MavenHome\bin;$env:Path"

Write-Host "Development environment loaded."
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "Node=$NodeHome"
Write-Host "Maven=$MavenHome"
Write-Host "Maven repository=$MavenRepository"
