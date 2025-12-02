chmod +x upload-update.sh

# رفع تحديث عادي
./upload-update.sh app-1.0.1.jar "1.0.1" \
    "إصلاح مشاكل الاتصال وتحسين الأداء" \
    "Fixed connection issues and improved performance" \
    false "1.0.0"

# رفع تحديث إجباري
./upload-update.sh app-2.0.0.jar "2.0.0" \
    "تحديث رئيسي مع ميزات جديدة" \
    "Major update with new features" \
    true "1.0.0"
