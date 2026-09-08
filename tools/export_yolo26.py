from pathlib import Path
import shutil
from ultralytics import YOLO

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
ASSETS.mkdir(parents=True, exist_ok=True)

print("Loading pretrained YOLO26n...")
model = YOLO("yolo26n.pt")

print("Exporting to ONNX (640x640)...")
result = model.export(
    format="onnx",
    imgsz=640,
    dynamic=False,
    simplify=True,
    nms=False,
)

source = Path(str(result))

if not source.exists():
    candidates = list(Path.cwd().rglob("yolo26n.onnx"))
    if not candidates:
        raise SystemExit("Export completed, but yolo26n.onnx was not found.")
    source = candidates[0]

target = ASSETS / "yolo26n.onnx"
shutil.copy2(source, target)

print(f"Copied model to: {target}")
print(f"Size: {target.stat().st_size / (1024*1024):.1f} MB")
print("Model preparation complete.")
