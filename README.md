# Oni Browser

Trình duyệt Android tối giản, giao diện lấy cảm hứng terminal Linux (nền đen,
chữ monospace, tab strip kiểu window manager), dùng WebView của hệ thống —
tương đương chức năng cơ bản của Chrome: nhiều tab, back/forward/reload,
address bar kiêm ô search, bookmark, desktop mode (giả UA Chrome desktop),
download file, clear data.

## Cấu trúc project (Gradle Android chuẩn)

```
OniBrowser/
├── build.gradle                 # root
├── settings.gradle
├── gradle.properties
├── local.properties.example     # đổi tên -> local.properties, sửa sdk.dir
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/oni/browser/
        │   ├── MainActivity.java     # logic chính: tab, nav, menu
        │   ├── TabData.java          # model 1 tab
        │   └── BookmarkStore.java    # lưu bookmark qua SharedPreferences
        ├── res/
        │   ├── layout/activity_main.xml, tab_item.xml
        │   ├── drawable/*.xml        # icon vector + background
        │   ├── mipmap-anydpi-v26/ic_launcher.xml
        │   └── values/colors.xml, strings.xml, styles.xml
        └── assets/start.html         # trang chủ kiểu terminal
```

Đây là cấu trúc Gradle Android **hợp lệ, chuẩn** — có thể mở trực tiếp bằng
Android Studio trên PC và build ngay (`Build > Build APK`).

## Build trên điện thoại (Termux) — 3 cách, xếp theo độ khả thi

APK Android cần Android Gradle Plugin + Android SDK (aapt2, d8, build-tools),
không phải chỉ `gradle` không thôi. Termux gói `pkg install gradle aapt aapt2`
**không đủ** để chạy AGP đầy đủ. Có 3 hướng thực tế:

### 1. AndroidIDE (khuyên dùng nhất, ít công nhất)
App Android riêng, chuyên để code + build APK ngay trên máy, tự quản lý SDK.
- Cài AndroidIDE (F-Droid hoặc GitHub release).
- Copy thư mục `OniBrowser` vào bộ nhớ máy, mở project trong AndroidIDE.
- Build APK ngay trong app, không cần lệnh gì thêm.

### 2. proot-distro Ubuntu trong Termux (kiểm soát nhiều hơn, nặng hơn)
```bash
pkg install proot-distro
proot-distro install ubuntu
proot-distro login ubuntu

# trong Ubuntu:
apt update && apt install -y openjdk-17-jdk-headless unzip wget
# tải Android cmdline-tools từ developer.android.com/studio -> giải nén
# dùng sdkmanager cài: "platform-tools" "platforms;android-33" "build-tools;33.0.2"
# tạo local.properties trỏ sdk.dir vào đường dẫn sdk vừa cài
cd OniBrowser
gradle assembleDebug
# APK ra ở app/build/outputs/apk/debug/app-debug.apk
```

### 3. GitHub Actions (nhẹ máy nhất, cần mạng + tài khoản GitHub) ⭐

Workflow đã có sẵn ở `.github/workflows/build.yml` — chỉ cần push code lên,
GitHub sẽ tự build APK và cho bạn tải về, không tốn tài nguyên máy bạn.

**Bước làm trong Termux:**

```bash
pkg install git

cd OniBrowser
git init
git add .
git commit -m "Oni Browser initial commit"

# tạo repo mới trên github.com trước (repo rỗng, không cần README/license)
git branch -M main
git remote add origin https://github.com/<username>/<repo>.git
git push -u origin main
```

**Lấy APK:**
1. Vào repo trên GitHub → tab **Actions**.
2. Chờ workflow "Build APK" chạy xong (vài phút, có dấu tick xanh).
3. Bấm vào lần chạy đó → kéo xuống mục **Artifacts** → tải
   `OniBrowser-debug-apk`.
4. Giải nén file zip đó ra là được `app-debug.apk` — copy sang điện thoại
   và cài (nhớ bật "Cho phép cài từ nguồn không rõ" trong Settings).

**Ghi chú:** project không có file `gradlew` (vì không thể tạo file nhị
phân `gradle-wrapper.jar` từ môi trường build này), nên workflow dùng
action `gradle/actions/setup-gradle` để có sẵn lệnh `gradle` — không cần
lo thiếu wrapper.

## Tùy biến thêm

- Đổi màu/theme: `app/src/main/res/values/colors.xml`
- Đổi trang chủ: `app/src/main/assets/start.html` hoặc `home_url` trong `strings.xml`
- Thêm nút/tính năng: sửa `activity_main.xml` + `MainActivity.java`
- Icon app: `drawable/ic_launcher_fg.xml` (vector, dễ sửa path)

## Hạn chế hiện tại (chưa làm)

- Chưa có file chooser cho input `type="file"` trong web (upload ảnh...).
- Chưa có history riêng (chỉ có back/forward của từng WebView + bookmark).
- Chưa có settings persist qua SharedPreferences cho desktop mode (reset khi mở lại app).
- Chưa hỗ trợ extension/ad-block — có thể thêm bằng cách chặn request trong `WebViewClient.shouldInterceptRequest`.
