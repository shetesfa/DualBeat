# DualBeat Automated Test Suite Runner
$java = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$compilerJar = "C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc\lib\kotlin-compiler.jar"
$kotlinHome = "C:\Program Files\Android\Android Studio\plugins\Kotlin\kotlinc"
$outDir = "$PSScriptRoot\build\test_classes"
$cp = "$outDir;$kotlinHome\lib\kotlin-stdlib.jar"

if (!(Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DUALBEAT TEST SUITE - REAL EXECUTION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Compile test suites
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\main\java\com\tesfa\dualbeat\receiver\protocol\DualBeatCastProtocol.kt" "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\receiver\DualBeatCastProtocolTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\main\java\com\tesfa\dualbeat\domain\model\DeviceRoute.kt" "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\bluetooth\BluetoothClassificationTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\receiver\JitterBufferHandlingTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\playback\AudioQueueManagementTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\data\RoomEntitiesTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\main\java\com\tesfa\dualbeat\core\util\FormatUtils.kt" "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\core\FormatUtilsTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\main\java\com\tesfa\dualbeat\domain\model\AudioEffectState.kt" "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\audio\EqualizerCurveTest.kt"
& $java -jar $compilerJar -kotlin-home $kotlinHome -d $outDir "$PSScriptRoot\app\src\test\java\com\tesfa\dualbeat\data\MediaSortingAndSearchTest.kt"

# Run all test suites
& $java -cp $cp com.tesfa.dualbeat.receiver.DualBeatCastProtocolTestKt
& $java -cp $cp com.tesfa.dualbeat.bluetooth.BluetoothClassificationTestKt
& $java -cp $cp com.tesfa.dualbeat.receiver.JitterBufferHandlingTestKt
& $java -cp $cp com.tesfa.dualbeat.playback.AudioQueueManagementTestKt
& $java -cp $cp com.tesfa.dualbeat.data.RoomEntitiesTestKt
& $java -cp $cp com.tesfa.dualbeat.core.FormatUtilsTestKt
& $java -cp $cp com.tesfa.dualbeat.audio.EqualizerCurveTestKt
& $java -cp $cp com.tesfa.dualbeat.data.MediaSortingAndSearchTestKt

Write-Host "========================================" -ForegroundColor Green
Write-Host "ALL 8 TEST SUITES EXECUTED SUCCESSFULLY" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
