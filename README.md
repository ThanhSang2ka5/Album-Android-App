# Album — Ứng Dụng Quản Lý Ảnh Trên Android

> **Bài tập 2 — Học phần Lập trình Thiết bị Di động**  
> **Nhóm 2:** Nguyễn Thanh Sang · Dương Việt Hưng · Nguyễn Minh Hiển

## Tính năng chính
1. **Lưu trữ hình ảnh an toàn & đọc thông số EXIF**:
   - Nhập nhiều ảnh từ thư viện thiết bị qua Android Photo Picker (`PickMultipleVisualMedia`).
   - Chụp ảnh trực tiếp từ Camera qua `FileProvider`.
   - Sao chép độc lập vào bộ nhớ riêng (`filesDir/photos/`), lưu siêu dữ liệu vào cơ sở dữ liệu **Room SQLite** và tự động trích xuất thông số **EXIF**.
   - Xem ảnh toàn màn hình hỗ trợ cử chỉ phóng đại 2 ngón (Pinch-to-Zoom 1x–4x), xoay ảnh, sửa tiêu đề/ghi chú và xem bảng chi tiết EXIF.
2. **Hai chế độ hiển thị trực quan**:
   - **Dòng thời gian (Timeline)**: Tự động gom nhóm theo Ngày hoặc Tháng, tìm kiếm tức thời theo tên/ghi chú/địa điểm, bộ lọc nhanh và chuyển đổi lưới 2–4 cột.
   - **Bộ sưu tập (Albums)**: Kết hợp Bộ sưu tập thông minh (*Yêu thích*, *7 ngày qua*, *Chưa xếp*) và quản lý Album cá nhân (tạo mới, đổi tên, xóa, chọn hàng loạt ảnh để thêm vào album).
3. **Chia sẻ trực tiếp lên Mạng xã hội**:
   - Định tuyến chia sẻ trực tiếp sang **Facebook**, **Twitter / X**, **Messenger**, **Instagram**, **Zalo** hoặc hộp thoại hệ thống thông qua `FileProvider` (`content://`) + `FLAG_GRANT_READ_URI_PERMISSION`.
   - Tích hợp công cụ tạo Caption + Hashtag và tự động sao chép vào Clipboard.
4. **Trình chiếu ảnh (Slideshow) & Nhạc nền bền vững**:
   - 4 nguồn chọn ảnh: Tự chọn từng ảnh, Theo Album, Theo khoảng thời gian (Từ ngày – Đến ngày) hoặc Toàn bộ kho ảnh.
   - 4 hiệu ứng chuyển cảnh mượt mà: *Ken Burns*, *Mờ dần (Crossfade)*, *Trượt ngang (Slide)* và *Thu phóng 3D*.
   - Tích hợp 3 giai điệu WAV sẵn có và hỗ trợ tải file nhạc cá nhân (`.MP3`/`.WAV`) lưu vĩnh viễn vào `filesDir/music/custom/` + `SharedPreferences`.

## Công nghệ & Thư viện mã nguồn mở (Apache 2.0)
- **Ngôn ngữ**: Kotlin 2.0.21 + Coroutines & Flow
- **Giao diện**: Jetpack Compose + Material Design 3
- **Kiến trúc**: MVVM (Model – View – ViewModel)
- **Cơ sở dữ liệu**: AndroidX Room ORM (SQLite)
- **Tải ảnh & EXIF**: Coil Compose 2.6.0, AndroidX ExifInterface 1.3.7, FileProvider

## Hướng dẫn chạy dự án
1. Mở thư mục dự án bằng **Android Studio**.
2. Chờ Gradle đồng bộ (Gradle 8.11.1, Android SDK 35).
3. Nhấn **Run** (hoặc chạy `./gradlew assembleDebug`) để cài đặt và chạy trên thiết bị thật hoặc Android Emulator (API 26+).
