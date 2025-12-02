#!/bin/bash

# upload-update.sh
# سكريبت لرفع التحديثات للسيرفر

SERVER_URL="http://164.92.230.242:8080"

echo "======================================"
echo "🚀 أداة رفع التحديثات"
echo "======================================"
echo ""

# التحقق من المعاملات
if [ "$#" -lt 4 ]; then
    echo "الاستخدام: ./upload-update.sh <jar-file> <version> <changelog-ar> <changelog-en> [required] [min-version]"
    echo ""
    echo "مثال:"
    echo "  ./upload-update.sh app-1.0.1.jar 1.0.1 \"إصلاحات وتحسينات\" \"Bug fixes\" false 1.0.0"
    exit 1
fi

JAR_FILE=$1
VERSION=$2
CHANGELOG_AR=$3
CHANGELOG_EN=$4
REQUIRED=${5:-false}
MIN_VERSION=$6

# التحقق من وجود الملف
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ الملف غير موجود: $JAR_FILE"
    exit 1
fi

echo "📁 الملف: $JAR_FILE"
echo "📌 الإصدار: $VERSION"
echo "✏️  التغييرات (عربي): $CHANGELOG_AR"
echo "✏️  التغييرات (English): $CHANGELOG_EN"
echo "⚠️  إجباري: $REQUIRED"
[ -n "$MIN_VERSION" ] && echo "📊 الحد الأدنى: $MIN_VERSION"
echo ""
echo "جاري الرفع..."
echo ""

# بناء الطلب
CURL_CMD="curl -X POST \"$SERVER_URL/api/admin/upload\" \
  -F \"file=@$JAR_FILE\" \
  -F \"version=$VERSION\" \
  -F \"changelogAr=$CHANGELOG_AR\" \
  -F \"changelogEn=$CHANGELOG_EN\" \
  -F \"required=$REQUIRED\""

[ -n "$MIN_VERSION" ] && CURL_CMD="$CURL_CMD -F \"minSupportedVersion=$MIN_VERSION\""

# تنفيذ الطلب
RESPONSE=$(eval $CURL_CMD -w "\n%{http_code}" -s)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo ""

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ تم رفع التحديث بنجاح!"
    echo ""
    echo "النتيجة:"
    echo "$BODY" | jq .
else
    echo "❌ فشل الرفع (HTTP $HTTP_CODE)"
    echo ""
    echo "الخطأ:"
    echo "$BODY" | jq .
    exit 1
fi
