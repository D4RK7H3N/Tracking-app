# Extracts Material Icons vector path data from the Compose
# material-icons-extended AAR and emits Android VectorDrawable XML.
#
# Why: adding material-icons-extended as a dependency ships all ~2100 icons
# (the release build does not run R8/resource shrinking). Extracting the path
# data instead lets the app use pixel-accurate Material art in ~1 KB local
# drawables with no dependency at all.
#
# Usage:
#   .\extract-material-icons.ps1 -Aar <path-to-material-icons-extended.aar> `
#       -OutDir <res\drawable> -Icons DirectionsBus,ElectricRickshaw,...
#
# The AAR is downloaded from:
#   https://dl.google.com/dl/android/maven2/androidx/compose/material/
#     material-icons-extended-android/1.7.6/
#     material-icons-extended-android-1.7.6.aar
#
# Material Icons are licensed under Apache License 2.0
# (https://github.com/google/material-design-icons).

param(
    [Parameter(Mandatory = $true)][string]$Aar,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [Parameter(Mandatory = $true)][string]$Icons,
    [string]$Javap = "javap",
    [string]$Package = "androidx.compose.material.icons.filled",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression.FileSystem

# Compose PathBuilder method -> Android VectorDrawable pathData command.
# Android's PathParser supports relative variants (m l h v c s q t a z),
# so relative Compose calls map 1:1.
$CommandMap = @{
    'moveTo'                 = @{ Cmd = 'M'; Args = 2 }
    'moveToRelative'         = @{ Cmd = 'm'; Args = 2 }
    'lineTo'                 = @{ Cmd = 'L'; Args = 2 }
    'lineToRelative'         = @{ Cmd = 'l'; Args = 2 }
    'relativeLineTo'         = @{ Cmd = 'l'; Args = 2 }
    'horizontalLineTo'       = @{ Cmd = 'H'; Args = 1 }
    'horizontalLineToRelative' = @{ Cmd = 'h'; Args = 1 }
    'verticalLineTo'         = @{ Cmd = 'V'; Args = 1 }
    'verticalLineToRelative' = @{ Cmd = 'v'; Args = 1 }
    'curveTo'                = @{ Cmd = 'C'; Args = 6 }
    'curveToRelative'        = @{ Cmd = 'c'; Args = 6 }
    'reflectiveCurveTo'      = @{ Cmd = 'S'; Args = 4 }
    'reflectiveCurveToRelative' = @{ Cmd = 's'; Args = 4 }
    'quadTo'                 = @{ Cmd = 'Q'; Args = 4 }
    'quadToRelative'         = @{ Cmd = 'q'; Args = 4 }
    'reflectiveQuadTo'       = @{ Cmd = 'T'; Args = 2 }
    'arcTo'                  = @{ Cmd = 'A'; Args = 7 }
    'close'                  = @{ Cmd = 'Z'; Args = 0 }
}

function Format-Float([double]$v) {
    $r = [Math]::Round($v, 4)
    if ($r -eq [Math]::Floor($r) -and [Math]::Abs($r) -lt 1e9) {
        return ([int]$r).ToString()
    }
    return $r.ToString('0.####', [Globalization.CultureInfo]::InvariantCulture)
}

function Convert-IconToPathData([string]$classFile) {
    $lines = & $Javap -c -p $classFile 2>&1
    $stack = New-Object System.Collections.Generic.List[double]
    $out = New-Object System.Collections.Generic.List[string]
    $cur = @{ x = 0.0; y = 0.0 }
    $unmapped = New-Object System.Collections.Generic.List[string]

    foreach ($line in $lines) {
        $t = $line.Trim()
        # javap prefixes every instruction with its bytecode offset.
        if ($t -match '^\d+:\s+(.*)$') { $t = $Matches[1].Trim() }

        # float constant pushes
        if ($t -match '^(?:ldc|ldc_w)\s+#\d+\s+//\s+float\s+(-?[\d.]+)f?$') {
            $stack.Add([double]$Matches[1]); continue
        }
        if ($t -match '^fconst_0$') { $stack.Add(0.0); continue }
        if ($t -match '^fconst_1$') { $stack.Add(1.0); continue }
        if ($t -match '^fconst_2$') { $stack.Add(2.0); continue }

        # PathBuilder call
        if ($t -match 'PathBuilder\.(\w+):\(') {
            $name = $Matches[1]
            # getNodes/addPathNodes are the parser round-trip, not path commands.
            if ($name -in @('getNodes', 'addPathNodes')) { $stack.Clear(); continue }
            if (-not $CommandMap.ContainsKey($name)) {
                if (-not $unmapped.Contains($name)) { $unmapped.Add($name) }
                $stack.Clear(); continue
            }
            $spec = $CommandMap[$name]
            $n = $spec.Args
            if ($stack.Count -lt $n) {
                throw "Not enough operands for $name in $classFile (have $($stack.Count), need $n)"
            }
            $vals = $stack.GetRange($stack.Count - $n, $n)
            $stack.Clear()
            $args = ($vals | ForEach-Object { Format-Float $_ }) -join ','
            $cmd = $spec.Cmd
            if ($n -gt 0) { $cmd = "$cmd $args" }
            $out.Add($cmd)

            # Track the current point so relative commands stay correct.
            switch ($name) {
                'moveTo'                 { $cur.x = $vals[0]; $cur.y = $vals[1] }
                'moveToRelative'         { $cur.x += $vals[0]; $cur.y += $vals[1] }
                'lineTo'                 { $cur.x = $vals[0]; $cur.y = $vals[1] }
                'lineToRelative'         { $cur.x += $vals[0]; $cur.y += $vals[1] }
                'relativeLineTo'         { $cur.x += $vals[0]; $cur.y += $vals[1] }
                'horizontalLineTo'       { $cur.x = $vals[0] }
                'horizontalLineToRelative' { $cur.x += $vals[0] }
                'verticalLineTo'         { $cur.y = $vals[0] }
                'verticalLineToRelative' { $cur.y += $vals[0] }
                'curveTo'                { $cur.x = $vals[4]; $cur.y = $vals[5] }
                'curveToRelative'        { $cur.x += $vals[4]; $cur.y += $vals[5] }
                'reflectiveCurveTo'      { $cur.x = $vals[2]; $cur.y = $vals[3] }
                'reflectiveCurveToRelative' { $cur.x += $vals[2]; $cur.y += $vals[3] }
                'quadTo'                 { $cur.x = $vals[2]; $cur.y = $vals[3] }
                'quadToRelative'         { $cur.x += $vals[2]; $cur.y += $vals[3] }
                'reflectiveQuadTo'       { $cur.x = $vals[0]; $cur.y = $vals[1] }
                'arcTo'                  { $cur.x = $vals[5]; $cur.y = $vals[6] }
            }
            continue
        }

        # Anything that is not a float push invalidates pending operands.
        if ($t -match '^(pop|aload|astore|dup|invokespecial|invokeinterface|invokestatic|ldc\s+#\d+\s+//\s+(?!float)|getfield|new\b)') {
            $stack.Clear()
        }
    }
    if ($unmapped.Count -gt 0) {
        Write-Warning ("Unmapped PathBuilder methods in {0}: {1}" -f $classFile, ($unmapped -join ', '))
    }
    return ($out -join ' ')
}

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }

$work = Join-Path ([IO.Path]::GetTempPath()) ("rea-icons-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $work | Out-Null
try {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Aar)
    $jarEntry = $zip.Entries | Where-Object { $_.FullName -eq 'classes.jar' }
    if (-not $jarEntry) { throw "classes.jar not found in $Aar" }
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($jarEntry, (Join-Path $work 'classes.jar'), $true)
    $zip.Dispose()

    $jar = [System.IO.Compression.ZipFile]::OpenRead((Join-Path $work 'classes.jar'))
    foreach ($icon in $Icons.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }) {
        $entryName = ($Package -replace '\.', '/') + "/$($icon)Kt.class"
        $entry = $jar.Entries | Where-Object { $_.FullName -eq $entryName }
        if (-not $entry) { Write-Warning "$icon : not found in $Package"; continue }
        $classFile = Join-Path $work "$($icon)Kt.class"
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $classFile, $true)

        $pathData = Convert-IconToPathData $classFile
        if (-not $pathData) { Write-Warning "$icon : empty path data"; continue }

        $snake = ($icon -creplace '([a-z0-9])([A-Z])', '$1_$2').ToLower()
        $xml = @"
<?xml version="1.0" encoding="utf-8"?>
<!-- Material Icons "$icon" (24dp) - Apache License 2.0.
     Path data extracted from androidx.compose.material:material-icons-extended
     by tools/extract-material-icons.ps1 so the app ships no icon dependency. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="$pathData" />
</vector>
"@
        $outFile = Join-Path $OutDir "ic_$snake.xml"
        if ((Test-Path $outFile) -and -not $Force) {
            Write-Warning "$($icon) : $outFile already exists, skipped (use -Force to overwrite)"
            continue
        }
        Set-Content -Path $outFile -Value $xml -Encoding UTF8
        $len = $pathData.Length
        Write-Host ("{0,-24} -> ic_{1}.xml  ({2} path chars)" -f $icon, $snake, $len)
    }
    $jar.Dispose()
}
finally {
    Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
}
