$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "Preparing YOLO26n ONNX model..." -ForegroundColor Cyan

$backendPython = Join-Path $root "..\visiontrack-ai\backend\.venv\Scripts\python.exe"
if (Test-Path $backendPython) {
    Write-Host "Using existing VisionTrack backend virtual environment."
    & $backendPython (Join-Path $PSScriptRoot "export_yolo26.py")
}
elseif (Get-Command python -ErrorAction SilentlyContinue) {
    python -m pip install --upgrade ultralytics
    python (Join-Path $PSScriptRoot "export_yolo26.py")
}
elseif (Get-Command py -ErrorAction SilentlyContinue) {
    py -3.12 -m pip install --upgrade ultralytics
    py -3.12 (Join-Path $PSScriptRoot "export_yolo26.py")
}
else {
    throw "Python was not found. Install Python 3.12 or run the export from your existing backend venv."
}

Write-Host ""
Write-Host "ONNX model ready. Open the project in Android Studio and build/run it." -ForegroundColor Green
