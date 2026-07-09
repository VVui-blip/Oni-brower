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

### 3. GitHub Actions (nhẹ máy nhất, cần mạng + tài khoản GitHub)
Push project này lên 1 repo, thêm workflow build APK bằng
`actions/setup-java` + `gradle assembleDebug`, để GitHub build hộ rồi
download APK từ tab Artifacts. Không tốn tài nguyên máy bạn.

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
