# GraphWalker offline — in day buoc ra stdout.
# Vi du (trong thu muc mbt-gw):
#   .\scripts\gw-offline.ps1 -ModelFile "01_AuthModel.json" -Generator 'random(edge_coverage(100) || length(30))' -StartElement v_GuestOnCourses
param(
    [Parameter(Mandatory = $true)]
    [string] $ModelFile,
    [Parameter(Mandatory = $true)]
    [string] $Generator,
    [string] $StartElement = ""
)

$ErrorActionPreference = "Stop"
$mbtGw = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $mbtGw "graphwalker-cli-4.3.3.jar"
if (-not (Test-Path $jar)) { throw "Khong thay: $jar" }

$modelsDir = Join-Path $mbtGw "src\test\resources\com\eduplatform\mbt\models"
$modelPath = $ModelFile
if (-not [System.IO.Path]::IsPathRooted($modelPath)) {
    $modelPath = Join-Path $modelsDir $ModelFile
}
if (-not (Test-Path $modelPath)) { throw "Khong thay file: $modelPath" }

$javaArgs = @("-jar", $jar, "offline", "-m", $modelPath, $Generator)
if ($StartElement -ne "") {
    $javaArgs += @("-e", $StartElement)
}
& java @javaArgs
