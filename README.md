# Vanilla

一个 Jetpack Compose Android 聊天界面原型，使用 `io.github.kyant0:backdrop` 实现 Liquid Glass 顶部组件、输入框、侧滑 A 界面设置按钮和设置页。

## 已实现

- 默认打开聊天界面，联系人名为 `friend`，头像为实色圆形填充。
- 顶部三个独立组件：左侧三条杠圆形按钮、中间头像+名字药丸、右侧加号圆形按钮。
- 底部胶囊聊天输入框：左侧无形状加号、右侧淡灰色圆形发送按钮；点击发送生成普通聊天气泡。
- 点击三条杠后，主聊天界面向右推开，左侧露出 A 界面。
- A 界面底部有悬浮液态玻璃设置按钮。
- 设置页里可调 UI 尺寸、侧滑宽度、输入框高度、玻璃模糊、折射、透明层、高光、内外阴影等参数。
- 气泡和发送按钮没有使用 Liquid Glass，符合需求。

## GitHub Actions 构建

把本目录上传到 GitHub 后，进入 Actions 手动运行 `Android Debug APK`，或 push 到 `main/master` 自动构建。APK 会作为 artifact 上传：`vanilla-debug-apk`。

## Termux 上传示例

```bash
unzip Vanilla.zip
cd Vanilla
git init
git add .
git commit -m "Initial liquid glass chat app"
git branch -M main
git remote add origin https://github.com/<你的用户名>/<你的仓库>.git
git push -u origin main
```
