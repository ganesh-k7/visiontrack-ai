# Push VisionTrack AI to GitHub

Run these commands from the repository root after creating an empty GitHub repository named:

```text
visiontrack-ai
```

Then:

```powershell
git init
git branch -M main
git add .
git status
git commit -m "Initial commit: VisionTrack AI"
git remote add origin https://github.com/YOUR_USERNAME/visiontrack-ai.git
git push -u origin main
```

Before `git add .`, verify this model is ignored:

```powershell
git status --short
```

You should **not** see:

```text
app/src/main/assets/yolo26n.onnx
```

After pushing, add screenshots to `docs/screenshots/` and update the screenshot section in the README.
