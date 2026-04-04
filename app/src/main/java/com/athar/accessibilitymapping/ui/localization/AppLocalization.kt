package com.athar.accessibilitymapping.ui.localization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale
import org.json.JSONObject

enum class AppLanguage(
  val code: String,
  val locale: Locale
) {
  English(code = "en", locale = Locale.ENGLISH),
  Arabic(code = "ar", locale = Locale.forLanguageTag("ar"));

  companion object {
    fun fromCode(code: String?): AppLanguage {
      return entries.firstOrNull { it.code == code } ?: English
    }
  }
}

val LocalAppLocalizer = staticCompositionLocalOf {
  AppLocalizer(language = AppLanguage.English, exactTranslations = emptyMap())
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }

@Composable
fun ProvideAppLocalization(
  language: AppLanguage,
  content: @Composable () -> Unit
) {
  val appContext = LocalContext.current.applicationContext
  val exactTranslations = remember(appContext) {
    loadArabicTranslations(appContext)
  }
  val localizer = remember(language, exactTranslations) {
    AppLocalizer(language = language, exactTranslations = exactTranslations)
  }
  val layoutDirection = if (language == AppLanguage.Arabic) {
    LayoutDirection.Rtl
  } else {
    LayoutDirection.Ltr
  }

  SideEffect {
    Locale.setDefault(language.locale)
  }

  CompositionLocalProvider(
    LocalLayoutDirection provides layoutDirection,
    LocalAppLanguage provides language,
    LocalAppLocalizer provides localizer,
    content = content
  )
}

@Composable
fun localized(text: String): String {
  return LocalAppLocalizer.current.localize(text)
}

class AppLocalizer(
  private val language: AppLanguage,
  private val exactTranslations: Map<String, String>
) {
  fun localize(rawText: String): String {
    if (language == AppLanguage.English || rawText.isBlank()) return rawText
    if (looksLikeMachineValue(rawText)) return rawText

    overrideTranslations[rawText]?.let { return it }
    exactTranslations[rawText]?.let { return fixBrandTerms(it) }

    dynamicPatterns.forEach { (pattern, transform) ->
      val match = pattern.matchEntire(rawText) ?: return@forEach
      return fixBrandTerms(transform(match))
    }

    return fixBrandTerms(applyTokenTranslations(rawText))
  }

  private fun applyTokenTranslations(rawText: String): String {
    var translated = rawText
    tokenTranslations.forEach { (source, target) ->
      translated = translated.replace(source, target, ignoreCase = false)
    }
    return translated
  }

  private fun fixBrandTerms(rawText: String): String {
    return rawText
      .replace("اثار", "أثر")
      .replace("ATHAR", "أثر")
      .replace("Athar", "أثر")
  }
}

private data class DynamicPattern(
  val pattern: Regex,
  val transform: (MatchResult) -> String
)

private val overrideTranslations = mapOf(
  // ── Brand ──
  "Athar" to "أثر",
  "ATHAR" to "أثر",
  "Athar v1.0.0 (MVP)" to "أثر الإصدار 1.0.0",
  "ATH-" to "أثر-",
  "ACCESSIBILITY FIRST" to "الوصول أولاً",
  "Athar Fees (30%)" to "رسوم أثر (30%)",
  "Mapping accessibility for everyone" to "نرسم خريطة الوصول للجميع",

  // ── Bottom navigation ──
  "Map" to "الخريطة",
  "Requests" to "الطلبات",
  "Dashboard" to "لوحة التحكم",
  "Prof\u200Cile" to "الملف الشخصي",
  "Profile" to "الملف الشخصي",
  "Map view" to "عرض الخريطة",
  "Requests view" to "عرض الطلبات",
  "Profile view" to "عرض الملف الشخصي",

  // ── Login / Auth ──
  "Welcome to Athar" to "مرحباً بك في أثر",
  "Making accessibility easier for everyone" to "نجعل الوصول أسهل للجميع",
  "Join Athar" to "انضم إلى أثر",
  "New to Athar?" to "جديد على أثر؟",
  "Already have an account? " to "لديك حساب بالفعل؟ ",
  "Don't have an account? " to "ليس لديك حساب؟ ",
  "By signing in, you agree to our Terms of Service and\nPrivacy Policy" to
    "بتسجيل الدخول، فإنك توافق على شروط الخدمة\nوسياسة الخصوصية الخاصة بنا",
  "Sign in" to "تسجيل الدخول",
  "Sign In" to "تسجيل الدخول",
  "Signing In..." to "جارٍ تسجيل الدخول...",
  "Log Out" to "تسجيل الخروج",
  "Log Out?" to "تسجيل الخروج؟",
  "Yes, Log Out" to "نعم، تسجيل الخروج",
  "Are you sure you want to log out of your account?" to "هل أنت متأكد من تسجيل الخروج من حسابك؟",
  "Remember me" to "تذكرني",
  "Forgot password?" to "نسيت كلمة المرور؟",
  "Register here" to "سجل هنا",
  "Enter your credentials to continue" to "أدخل بياناتك للمتابعة",
  "Email Address" to "البريد الإلكتروني",
  "Password" to "كلمة المرور",
  "Enter your password" to "أدخل كلمة المرور",
  "Hide password" to "إخفاء كلمة المرور",
  "Show password" to "إظهار كلمة المرور",
  "Please enter your email and password." to "يرجى إدخال البريد الإلكتروني وكلمة المرور.",
  "Password reset is not enabled yet. Please contact support." to "إعادة تعيين كلمة المرور غير متاحة حالياً. يرجى التواصل مع الدعم.",
  "Choose your role to get started" to "اختر دورك للبدء",

  // ── Role Selection ──
  "Find accessible places and get assistance when needed" to "ابحث عن أماكن يسهل الوصول إليها واحصل على المساعدة عند الحاجة",
  "Search accessible places" to "البحث عن أماكن يسهل الوصول إليها",
  "Request volunteer assistance" to "طلب مساعدة متطوع",
  "Rate locations & volunteers" to "تقييم المواقع والمتطوعين",
  "Become a volunteer and assist people in your community" to "كن متطوعاً وساعد الناس في مجتمعك",
  "Accept assistance requests" to "قبول طلبات المساعدة",
  "Go live when available" to "ابدأ البث عند التوفر",
  "Build your volunteer profile" to "أنشئ ملف المتطوع الخاص بك",

  // ── Registration ──
  "Full Name" to "الاسم الكامل",
  "Full name" to "الاسم الكامل",
  "Enter your full name" to "أدخل اسمك الكامل",
  "Next Step" to "الخطوة التالية",
  "Back" to "رجوع",
  "Create Account" to "إنشاء حساب",
  "Creating..." to "جارٍ الإنشاء...",
  "Riyadh, Saudi Arabia" to "الرياض، المملكة العربية السعودية",

  // ── Disability Types ──
  "Wheelchair user" to "مستخدم كرسي متحرك",
  "Visually impaired" to "ضعيف البصر",
  "Hearing impaired" to "ضعيف السمع",
  "Mobility challenges" to "تحديات حركية",
  "Cognitive disability" to "إعاقة ذهنية",
  "Multiple disabilities" to "إعاقات متعددة",
  "Prefer not to say" to "أفضل عدم الإفصاح",
  "Other" to "أخرى",

  // ── Splash Categories ──
  "Mobility" to "الحركة",
  "Vision" to "البصر",
  "Hearing" to "السمع",
  "Communication" to "التواصل",
  "Cognitive" to "الإدراك",

  // ── Map Screen ──
  "Accessible Places Nearby" to "أماكن يسهل الوصول إليها بالقرب",
  "Search for accessible places..." to "ابحث عن أماكن يسهل الوصول إليها...",
  "Loading map..." to "جارٍ تحميل الخريطة...",
  "Map is taking too long to load. Check API key restrictions and billing, or try a device with Google Play." to
    "تحميل الخريطة يستغرق وقتاً طويلاً. تحقق من قيود مفتاح API أو جرّب جهازاً مع Google Play.",
  "Add MAPS_API_KEY to local.properties or set it as an environment variable." to
    "أضف MAPS_API_KEY إلى local.properties أو عيّنه كمتغير بيئي.",
  "Filters" to "التصفية",
  "Ramps" to "منحدرات",
  "Elevators" to "مصاعد",
  "Accessible Parking" to "مواقف سيارات لذوي الإعاقة",
  "Braille" to "برايل",
  "Audio Guides" to "أدلة صوتية",
  "Get Help" to "طلب مساعدة",
  "Live" to "مباشر",
  "Go Live" to "ابدأ البث",
  "Pending Verification" to "بانتظار التحقق",
  "Search Result" to "نتيجة البحث",
  "Loading suggestions..." to "جارٍ تحميل الاقتراحات...",
  "No suggestions found." to "لا توجد اقتراحات.",
  "Searching..." to "جارٍ البحث...",
  "Search is not available on this device." to "البحث غير متاح على هذا الجهاز.",
  "Google Places is not ready yet. Please try again." to "خدمة أماكن Google غير جاهزة بعد. يرجى المحاولة مرة أخرى.",
  "Unable to load the selected place." to "تعذر تحميل المكان المحدد.",
  "Zoom in" to "تكبير",
  "Zoom out" to "تصغير",
  "Recenter map" to "إعادة توسيط الخريطة",
  "Directions to search result" to "الاتجاهات إلى نتيجة البحث",
  "Close location details" to "إغلاق تفاصيل الموقع",
  "Accessibility Features" to "ميزات الوصول",
  "Recent Updates" to "التحديثات الأخيرة",
  "Navigate Here" to "التنقل إلى هنا",
  "Rate & Report" to "تقييم وإبلاغ",
  "Unknown" to "غير معروف",

  // ── Volunteer Request (MapScreen) ──
  "Request Volunteer Help" to "طلب مساعدة متطوع",
  "Close request form" to "إغلاق نموذج الطلب",
  "Your Current Location" to "موقعك الحالي",
  "Central Mall, Main Entrance" to "المول المركزي، المدخل الرئيسي",
  "Use my current location" to "استخدم موقعي الحالي",
  "Where do you need to go?" to "إلى أين تحتاج الذهاب؟",
  "Type of Assistance Needed *" to "نوع المساعدة المطلوبة *",
  "Select help type..." to "اختر نوع المساعدة...",
  "Urgency Level *" to "مستوى الاستعجال *",
  "Additional Details (Optional)" to "تفاصيل إضافية (اختياري)",
  "Any additional information that might help the volunteer..." to "أي معلومات إضافية قد تساعد المتطوع...",
  "Your request will be broadcast to verified volunteers within 5km. You'll be matched with the nearest available volunteer." to
    "سيتم إرسال طلبك إلى المتطوعين المعتمدين في نطاق 5 كم. سيتم مطابقتك مع أقرب متطوع متاح.",
  "Please fill in all required fields." to "يرجى ملء جميع الحقول المطلوبة.",
  "Broadcasting Request..." to "جارٍ إرسال الطلب...",
  "Send Request" to "إرسال الطلب",
  "Only users with accessibility needs can request volunteer assistance." to
    "فقط المستخدمون ذوو احتياجات الوصول يمكنهم طلب مساعدة المتطوعين.",
  "Searching for nearby volunteers. Average response time is under 2 minutes." to
    "جارٍ البحث عن متطوعين قريبين. متوسط وقت الاستجابة أقل من دقيقتين.",
  "Navigation assistance" to "مساعدة في التنقل",
  "Finding accessible entrance" to "العثور على مدخل يسهل الوصول إليه",
  "Help with accessible transportation" to "مساعدة في النقل الميسّر",
  "Guide to specific location" to "دليل إلى موقع محدد",
  "Reading assistance" to "مساعدة في القراءة",
  "Other assistance" to "مساعدة أخرى",
  "Low" to "منخفض",
  "Medium" to "متوسط",
  "High" to "عالي",

  // ── Rating / Review ──
  "Please choose a rating before submitting." to "يرجى اختيار تقييم قبل الإرسال.",
  "Submitting..." to "جارٍ الإرسال...",
  "Submit Rating" to "إرسال التقييم",
  "Submit Review" to "إرسال المراجعة",
  "Rate your experience" to "قيّم تجربتك",
  "How would you rate this assistance?" to "كيف تقيّم هذه المساعدة؟",
  "Share your experience... (optional)" to "شارك تجربتك... (اختياري)",
  "Report an issue" to "الإبلاغ عن مشكلة",
  "Did anything go wrong?" to "هل حدث خطأ ما؟",
  "Describe the issue in more detail (optional)..." to "صف المشكلة بمزيد من التفصيل (اختياري)...",
  "Skip for now" to "تخطي الآن",
  "Thank you for your feedback!" to "شكراً على ملاحظاتك!",
  "Your review has been submitted successfully. It helps us improve the experience for everyone." to
    "تم إرسال مراجعتك بنجاح. تساعدنا في تحسين التجربة للجميع.",
  "Back to Home" to "العودة إلى الرئيسية",
  "Poor" to "سيء",
  "Fair" to "مقبول",
  "Good" to "جيد",
  "Great" to "رائع",
  "Very Good" to "جيد جداً",
  "Excellent" to "ممتاز",
  "Excellent!" to "ممتاز!",
  "Rate Experience" to "تقييم التجربة",

  // ── Profile Screen ──
  "Account Settings" to "إعدادات الحساب",
  "Notifications" to "الإشعارات",
  "Privacy & Security" to "الخصوصية والأمان",
  "Help & Support" to "المساعدة والدعم",
  "User Account" to "حساب مستخدم",
  "Volunteer Account" to "حساب متطوع",
  "Edit profile" to "تعديل الملف الشخصي",
  "Role Locked" to "الدور مقفل",
  "Your account role is permanent and can only be changed by admin verification. Contact support if you need to switch roles." to
    "دور حسابك دائم ولا يمكن تغييره إلا بتحقق المسؤول. تواصل مع الدعم إذا كنت تريد تغيير الدور.",
  "Sign Language Translator" to "مترجم لغة الإشارة",
  "Analytics Dashboard" to "لوحة التحليلات",

  // ── Account Settings ──
  "Personal Information" to "المعلومات الشخصية",
  "Phone Number" to "رقم الهاتف",
  "Location" to "الموقع",
  "Choose your app language" to "اختر لغة التطبيق",
  "Accessibility Information" to "معلومات الوصول",
  "This helps us provide better assistance and relevant accessibility information" to
    "يساعدنا ذلك في تقديم مساعدة أفضل ومعلومات وصول ذات صلة",
  "Accessibility Needs" to "احتياجات الوصول",
  "Select your accessibility needs..." to "حدد احتياجات الوصول الخاصة بك...",
  "Security" to "الأمان",
  "Save Changes" to "حفظ التغييرات",
  "Saving..." to "جارٍ الحفظ...",
  "Change Photo" to "تغيير الصورة",
  "Change Password" to "تغيير كلمة المرور",
  "Language" to "اللغة",
  "English" to "الإنجليزية",
  "Arabic" to "العربية",
  "Couldn't update your profile photo. Please try again." to "تعذر تحديث صورة ملفك الشخصي. يرجى المحاولة مرة أخرى.",

  // ── Change Password ──
  "At least 8 characters" to "8 أحرف على الأقل",
  "One uppercase letter" to "حرف كبير واحد",
  "One lowercase letter" to "حرف صغير واحد",
  "One number" to "رقم واحد",
  "One special character" to "رمز خاص واحد",
  "Updating..." to "جارٍ التحديث...",
  "All password fields are required." to "جميع حقول كلمة المرور مطلوبة.",
  "New password and confirmation do not match." to "كلمة المرور الجديدة والتأكيد غير متطابقتين.",
  "New password does not meet all requirements." to "كلمة المرور الجديدة لا تستوفي جميع المتطلبات.",
  "Current Password" to "كلمة المرور الحالية",
  "New Password" to "كلمة المرور الجديدة",
  "Confirm New Password" to "تأكيد كلمة المرور الجديدة",
  "Password Requirements:" to "متطلبات كلمة المرور:",

  // ── Notifications Screen ──
  "Push Notifications" to "إشعارات الدفع",
  "Receive notifications on your device" to "تلقي الإشعارات على جهازك",
  "New Requests" to "طلبات جديدة",
  "Get notified about new help requests" to "الإشعار بطلبات المساعدة الجديدة",
  "Request Updates" to "تحديثات الطلبات",
  "Status changes for your requests" to "تغييرات حالة طلباتك",
  "Messages" to "الرسائل",
  "New messages from volunteers or users" to "رسائل جديدة من المتطوعين أو المستخدمين",
  "Community Updates" to "تحديثات المجتمع",
  "News and updates about Athar" to "أخبار وتحديثات عن أثر",
  "Sound & Vibration" to "الصوت والاهتزاز",
  "Notification Sound" to "صوت الإشعار",
  "Vibration" to "الاهتزاز",
  "Quiet Hours" to "ساعات الهدوء",
  "Set Quiet Hours" to "تعيين ساعات الهدوء",
  "Mute notifications during specific times" to "كتم الإشعارات خلال أوقات محددة",

  // ── Privacy & Security ──
  "Privacy Settings" to "إعدادات الخصوصية",
  "Profile Visibility" to "رؤية الملف الشخصي",
  "Allow others to see your profile" to "السماح للآخرين برؤية ملفك الشخصي",
  "Location Sharing" to "مشاركة الموقع",
  "Share your location with volunteers" to "مشاركة موقعك مع المتطوعين",
  "Activity Status" to "حالة النشاط",
  "Show when you're online" to "إظهار حالتك عند الاتصال",
  "Two-Factor Authentication" to "المصادقة الثنائية",
  "Add an extra layer of security" to "إضافة طبقة أمان إضافية",
  "Biometric Login" to "تسجيل الدخول بالبصمة",
  "Use fingerprint or face to login" to "استخدم البصمة أو الوجه لتسجيل الدخول",
  "Login Notifications" to "إشعارات تسجيل الدخول",
  "Get notified of new logins" to "الإشعار بتسجيلات الدخول الجديدة",
  "Active Sessions" to "الجلسات النشطة",
  "Manage your active sessions" to "إدارة جلساتك النشطة",
  "Data & Privacy" to "البيانات والخصوصية",
  "Download My Data" to "تحميل بياناتي",
  "Get a copy of your personal data" to "الحصول على نسخة من بياناتك الشخصية",
  "Delete Account" to "حذف الحساب",
  "Permanently delete your account and data" to "حذف حسابك وبياناتك نهائياً",
  "Privacy Policy" to "سياسة الخصوصية",
  "Read our privacy policy" to "اقرأ سياسة الخصوصية",
  "Terms of Service" to "شروط الخدمة",
  "Read our terms of service" to "اقرأ شروط الخدمة",
  "We take your privacy seriously. Your data is encrypted and never shared without consent." to
    "نحن نأخذ خصوصيتك على محمل الجد. بياناتك مشفرة ولا تتم مشاركتها أبداً دون موافقتك.",

  // ── Help & Support ──
  "Get in Touch" to "تواصل معنا",
  "Contact Support" to "التواصل مع الدعم",
  "Email Support" to "الدعم عبر البريد",
  "Send us an email" to "أرسل لنا بريداً إلكترونياً",
  "Call Us" to "اتصل بنا",
  "Speak with a support agent" to "تحدث مع وكيل الدعم",
  "Live Chat" to "الدردشة المباشرة",
  "Chat with our support team" to "تحدث مع فريق الدعم",
  "Report a Bug" to "الإبلاغ عن خطأ",
  "Help us improve by reporting issues" to "ساعدنا في التحسين بالإبلاغ عن المشكلات",
  "Resources" to "الموارد",
  "User Guide" to "دليل المستخدم",
  "Learn how to use Athar" to "تعلم كيفية استخدام أثر",
  "Video Tutorials" to "دروس فيديو",
  "Watch step-by-step guides" to "شاهد أدلة خطوة بخطوة",
  "Documentation" to "التوثيق",
  "Technical documentation" to "التوثيق التقني",
  "Frequently Asked Questions" to "الأسئلة الشائعة",
  "Our support team typically responds within 24 hours. For urgent issues, please call our hotline." to
    "عادة يرد فريق الدعم خلال 24 ساعة. للمسائل العاجلة، يرجى الاتصال بخط الطوارئ.",
  "Please fill in all fields." to "يرجى ملء جميع الحقول.",
  "Sending..." to "جارٍ الإرسال...",
  "Send Message" to "إرسال رسالة",
  "Subject" to "الموضوع",
  "Your Message" to "رسالتك",

  // ── Requests Screen ──
  "Volunteer Requests" to "طلبات المتطوعين",
  "All" to "الكل",
  "Pending" to "قيد الانتظار",
  "Active" to "نشط",
  "History" to "السجل",
  "No Requests" to "لا توجد طلبات",
  "No requests yet. Create your first request from the Map view." to
    "لا توجد طلبات بعد. أنشئ أول طلب من عرض الخريطة.",
  "Cancel" to "إلغاء",
  "Volunteer: " to "المتطوع: ",
  "Pay for Service" to "الدفع مقابل الخدمة",
  "Volunteer contact details are private in this MVP." to "تفاصيل اتصال المتطوع خاصة في هذا الإصدار.",
  "Contact" to "اتصال",
  "Want to help?" to "تريد المساعدة؟",
  "Enable volunteer mode" to "تفعيل وضع المتطوع",
  "Enable volunteer mode from Account Settings." to "فعّل وضع المتطوع من إعدادات الحساب.",
  "Assistance" to "مساعدة",
  "Unable to submit review right now." to "تعذر إرسال المراجعة حالياً.",

  // ── Volunteer Dashboard ──
  "Volunteer Dashboard" to "لوحة تحكم المتطوع",
  "You're Offline" to "أنت غير متصل",
  "Go live from the Map view to start receiving assistance requests from people nearby." to
    "ابدأ البث من عرض الخريطة لتلقي طلبات المساعدة من الأشخاص القريبين.",
  "All Caught Up!" to "لا يوجد شيء جديد!",
  "No assistance requests at the moment. You'll be notified when someone nearby needs help." to
    "لا توجد طلبات مساعدة حالياً. سيتم إشعارك عندما يحتاج شخص قريب للمساعدة.",
  "No Active Assistance" to "لا توجد مساعدة نشطة",
  "Accept an incoming request to start helping someone." to "اقبل طلباً وارداً لبدء مساعدة شخص ما.",
  "No History Yet" to "لا يوجد سجل بعد",
  "Your completed assistance sessions will appear here." to "ستظهر جلسات المساعدة المكتملة هنا.",
  "Assistance in Progress" to "مساعدة قيد التنفيذ",
  "Incoming" to "وارد",
  "In Progress" to "قيد التنفيذ",
  "Completed" to "مكتمل",
  "Just now" to "الآن",
  "Accept" to "قبول",
  "Decline" to "رفض",
  "Complete" to "إكمال",
  "Navigate" to "تنقل",

  // ── Sign Language Translator ──
  "Glove Translator" to "مترجم القفاز",
  "Camera Translator" to "مترجم الكاميرا",
  "Glove Sensor" to "مستشعر القفاز",
  "Desktop Stream" to "بث سطح المكتب",
  "Athar Glove v2" to "قفاز أثر الإصدار 2",
  "Connected" to "متصل",
  "Connecting..." to "جارٍ الاتصال...",
  "Disconnected" to "غير متصل",
  "Go back" to "رجوع",
  "Bluetooth glove recognition" to "التعرف عبر قفاز البلوتوث",
  "Video stream recognition" to "التعرف عبر بث الفيديو",
  "Info" to "معلومات",
  "Connect the Athar smart glove via Bluetooth. The glove sensors detect finger positions and translate them into individual characters (A-Z, 0-9) in real time." to
    "قم بتوصيل قفاز أثر الذكي عبر البلوتوث. تكتشف مستشعرات القفاز أوضاع الأصابع وتترجمها إلى أحرف فردية (A-Z, 0-9) في الوقت الفعلي.",
  "Receive a live video stream from the Athar desktop app. The camera captures sign language gestures and translates them into words and phrases." to
    "استقبل بث فيديو مباشر من تطبيق أثر على سطح المكتب. تلتقط الكاميرا إيماءات لغة الإشارة وتترجمها إلى كلمات وعبارات.",
  "Turn on the Athar glove and enable Bluetooth" to "شغّل قفاز أثر وفعّل البلوتوث",
  "Tap \"Pair Glove\" to connect via Bluetooth" to "اضغط \"إقران القفاز\" للاتصال عبر البلوتوث",
  "Start listening - characters appear as you sign" to "ابدأ الاستماع - تظهر الأحرف أثناء الإشارة",
  "Open the Athar desktop app and start camera" to "افتح تطبيق أثر على سطح المكتب وابدأ الكاميرا",
  "Copy the stream URL and paste it below" to "انسخ رابط البث والصقه أدناه",
  "Tap Connect, then Start Translating" to "اضغط اتصال، ثم ابدأ الترجمة",
  "Got it" to "فهمت",
  "Stop" to "إيقاف",
  "Start Listening" to "بدء الاستماع",
  "Translated Text" to "النص المترجم",
  "Pair the glove and start listening to see characters here" to "أقرن القفاز وابدأ الاستماع لرؤية الأحرف هنا",
  "Camera Permission Required" to "إذن الكاميرا مطلوب",
  "Camera Ready" to "الكاميرا جاهزة",
  "Tap 'Start Detection' below" to "اضغط 'بدء الكشف' أدناه",
  "Fullscreen" to "شاشة كاملة",
  "Toggle fullscreen" to "تبديل الشاشة الكاملة",
  "Stop Detection" to "إيقاف الكشف",
  "Start Detection" to "بدء الكشف",
  "Clear" to "مسح",
  "Translation" to "الترجمة",
  "Copy" to "نسخ",
  "Start detection to see translations" to "ابدأ الكشف لرؤية الترجمات",
  "Listening" to "الاستماع",

  // ── SimpleCameraTranslator ──
  "Camera ready. Start detection to analyze supported gestures." to
    "الكاميرا جاهزة. ابدأ الكشف لتحليل الإيماءات المدعومة.",
  "Camera permission is required for live gesture recognition." to
    "إذن الكاميرا مطلوب للتعرف على الإيماءات المباشرة.",
  "No hand detected. Keep one hand centered and well lit." to
    "لم يتم اكتشاف يد. أبقِ يداً واحدة في المنتصف وبإضاءة جيدة.",
  "Hand detected. Try one of the supported gestures below." to
    "تم اكتشاف يد. جرب إحدى الإيماءات المدعومة أدناه.",
  "Camera permission required" to "إذن الكاميرا مطلوب",
  "Enable camera access to analyze live gestures." to "فعّل الوصول إلى الكاميرا لتحليل الإيماءات المباشرة.",
  "Grant Permission" to "منح الإذن",
  "Recognizer error" to "خطأ في التعرف",
  "Camera ready" to "الكاميرا جاهزة",
  "Enable Camera" to "تفعيل الكاميرا",
  "Transcript cleared. Start detection to analyze supported gestures." to
    "تم مسح النص. ابدأ الكشف لتحليل الإيماءات المدعومة.",
  "Allow camera access to start live recognition." to "اسمح بالوصول إلى الكاميرا لبدء التعرف المباشر.",
  "Waiting for interpretation..." to "في انتظار التفسير...",
  "Live camera gestures with English and Arabic output" to "إيماءات الكاميرا المباشرة مع مخرجات بالإنجليزية والعربية",
  "Unable to start the camera recognizer." to "تعذر بدء التعرف بالكاميرا.",
  "This now uses real camera frames and a real MediaPipe gesture model. It does not cover full sign-language vocabulary or guarantee 100% accuracy. Full Arabic and English sign-language translation needs a custom trained model and evaluation data." to
    "يستخدم هذا الآن إطارات كاميرا حقيقية ونموذج إيماءات MediaPipe حقيقي. لا يغطي مفردات لغة الإشارة الكاملة ولا يضمن دقة 100%. ترجمة لغة الإشارة الكاملة بالعربية والإنجليزية تحتاج نموذجاً مدرباً مخصصاً وبيانات تقييم.",

  // ── Add Place Report ──
  "Add a Place Report" to "إضافة تقرير عن مكان",
  "Add Place Report" to "إضافة تقرير عن مكان",
  "Report Submitted!" to "تم إرسال التقرير!",
  "Thank you for helping improve accessibility for everyone." to "شكراً لمساعدتك في تحسين الوصول للجميع.",
  "Submit Report" to "إرسال التقرير",
  "Rate This Place" to "قيّم هذا المكان",
  "How accessible is this location?" to "ما مدى سهولة الوصول إلى هذا الموقع؟",
  "Select all features available at this location." to "حدد جميع الميزات المتاحة في هذا الموقع.",
  "Select a location, governorate, and rating to continue" to "حدد موقعاً ومحافظة وتقييماً للمتابعة",

  // ── Volunteer Analytics ──
  "Net Earnings" to "صافي الأرباح",
  "Avg Rating" to "متوسط التقييم",
  "Current Month" to "الشهر الحالي",
  "Request Types" to "أنواع الطلبات",
  "No performance data yet" to "لا توجد بيانات أداء بعد",
  "Complete requests to unlock your volunteer performance score" to
    "أكمل الطلبات لفتح نقاط أداء المتطوع",
  "This Week" to "هذا الأسبوع",
  "This Month Net" to "صافي هذا الشهر",
  "No payment history yet." to "لا يوجد سجل دفع بعد.",
  "Enter amount" to "أدخل المبلغ",
  "Withdraw" to "سحب",
  "Performance" to "الأداء",
  "Earnings" to "الأرباح",
  "Total Assists" to "إجمالي المساعدات",
  "Response Time" to "وقت الاستجابة",
  "This Month" to "هذا الشهر",
  "Total Earned" to "إجمالي المكتسب",
  "Total Fees" to "إجمالي الرسوم",
  "Payment History" to "سجل الدفعات",
  "Gross Amount" to "المبلغ الإجمالي",
  "Athar Fee (30%)" to "رسوم أثر (٣٠٪)",
  "Net (You Received)" to "الصافي (ما استلمته)",

  // ── Payment Flow ──
  "Service" to "الخدمة",
  "Date" to "التاريخ",
  "From" to "من",
  "To" to "إلى",
  "Close" to "إغلاق",
  "Paymob" to "Paymob",
  "Cash" to "نقداً",
  "Payment" to "الدفع",
  "Accepted" to "مقبول",
  "Number of Hours" to "عدد الساعات",
  "Price Per Hour (EGP)" to "سعر الساعة (جنيه مصري)",
  "hour" to "ساعة",
  "hours" to "ساعات",
  "Order Details" to "تفاصيل الطلب",
  "Total Amount" to "المبلغ الإجمالي",
  "Total Paid" to "إجمالي المدفوع",
  "Hours" to "الساعات",
  "Rate" to "السعر",
  "Paymob checkout link is unavailable." to "رابط الدفع عبر Paymob غير متاح.",
  "Opening Paymob checkout." to "جارٍ فتح صفحة الدفع عبر Paymob.",
  "Unable to open Paymob checkout on this device." to "تعذر فتح صفحة الدفع عبر Paymob على هذا الجهاز.",
  "Complete payment in Paymob, then return to the app." to "أكمل الدفع في Paymob، ثم عُد إلى التطبيق.",
  "Open the checkout, finish the payment, then confirm it below." to
    "افتح صفحة الدفع، أنهِ الدفع، ثم أكده أدناه.",
  "You'll be redirected to Paymob's secure checkout." to "ستتم إعادة توجيهك إلى صفحة الدفع الآمنة لـ Paymob.",

  // ── Governorates ──
  "Governorate" to "المحافظة",
  "Select the governorate where this place is located." to "حدد المحافظة التي يقع فيها هذا المكان.",
  "Select Governorate" to "اختر المحافظة",
  "Cairo" to "القاهرة",
  "Alexandria" to "الإسكندرية",
  "Giza" to "الجيزة",
  "Qalyubia" to "القليوبية",
  "Port Said" to "بورسعيد",
  "Suez" to "السويس",
  "Dakahlia" to "الدقهلية",
  "Sharqia" to "الشرقية",
  "Gharbia" to "الغربية",
  "Monufia" to "المنوفية",
  "Beheira" to "البحيرة",
  "Kafr El Sheikh" to "كفر الشيخ",
  "Damietta" to "دمياط",
  "Ismailia" to "الإسماعيلية",
  "Fayoum" to "الفيوم",
  "Beni Suef" to "بني سويف",
  "Minya" to "المنيا",
  "Assiut" to "أسيوط",
  "Sohag" to "سوهاج",
  "Qena" to "قنا",
  "Luxor" to "الأقصر",
  "Aswan" to "أسوان",
  "Red Sea" to "البحر الأحمر",
  "New Valley" to "الوادي الجديد",
  "Matrouh" to "مطروح",
  "North Sinai" to "شمال سيناء",
  "South Sinai" to "جنوب سيناء",

  // ── Download Data ──
  "Download Your Data" to "تحميل بياناتك",
  "Request Data Download" to "طلب تحميل البيانات",
  "Done" to "تم",

  // ── Logout Dialog ──
  "Athar Logo" to "شعار أثر",
  "Athar logo" to "شعار أثر",

  // ── User Guide ──
  "How to Use Athar" to "كيفية استخدام أثر",
  "This guide helps you get the most out of Athar. Tap any section below to expand detailed instructions." to
    "يساعدك هذا الدليل في الاستفادة القصوى من أثر. اضغط على أي قسم أدناه لتوسيع التعليمات المفصلة.",
  "Quick Tips" to "نصائح سريعة",
  "Still Need Help?" to "هل ما زلت بحاجة للمساعدة؟",
  "If you could not find what you were looking for, check video tutorials or contact support." to
    "إذا لم تجد ما تبحث عنه، تحقق من دروس الفيديو أو تواصل مع الدعم.",

  // ── Video Tutorials ──
  "Learn by Watching" to "تعلم بالمشاهدة",
  "Watch step-by-step guides to learn all Athar features. Great for visual learners." to
    "شاهد أدلة خطوة بخطوة لتعلم جميع ميزات أثر. رائعة للمتعلمين بالمشاهدة.",
  "Categories" to "الفئات",
  "No Tutorials Found" to "لم يتم العثور على دروس",
  "Try adjusting your search or category filter." to "حاول تعديل البحث أو فلتر الفئة.",
  "More Videos Coming Soon" to "المزيد من الفيديوهات قريباً",
  "New tutorial videos are added regularly. Check back for advanced features and tips." to
    "تُضاف فيديوهات تعليمية جديدة بانتظام. عُد لاحقاً للميزات المتقدمة والنصائح.",
  "Tap any video card to watch the tutorial" to "اضغط على أي بطاقة فيديو لمشاهدة الدرس",

  // ── Register fields ──
  "Weekday mornings" to "صباح أيام العمل",
  "Weekday afternoons" to "بعد ظهر أيام العمل",
  "Weekday evenings" to "مساء أيام العمل",
  "Weekend mornings" to "صباح عطلة نهاية الأسبوع",
  "Weekend afternoons" to "بعد ظهر عطلة نهاية الأسبوع",
  "Weekend evenings" to "مساء عطلة نهاية الأسبوع",
  "Flexible/On-demand" to "مرن/حسب الطلب",
  "French" to "الفرنسية",
  "Urdu" to "الأردية",
  "Hindi" to "الهندية",
  "Enter your ID number" to "أدخل رقم هويتك",
  "Tap to upload ID document" to "اضغط لرفع وثيقة الهوية",

  // ── Misc ──
  "User" to "مستخدم",
  "Volunteer" to "متطوع",
  "OK" to "حسناً",
  "Confirm" to "تأكيد",
  "Yes" to "نعم",
  "No" to "لا",
  "Error" to "خطأ",
  "Success" to "نجاح",
  "Loading..." to "جارٍ التحميل...",
  "Retry" to "إعادة المحاولة",
  "Search" to "بحث",
  "Settings" to "الإعدادات",
  "Home" to "الرئيسية",
  "Request Broadcasted!" to "تم إرسال الطلب!",
  "Pair Glove" to "إقران القفاز",
  "Open in Browser" to "فتح في المتصفح",

  // ── Role Selection (extra) ──
  "I Need Help" to "أحتاج مساعدة",
  "I Want to Help" to "أريد المساعدة",

  // ── Register User ──
  "Create User Account" to "إنشاء حساب مستخدم",
  "For people who need assistance" to "للأشخاص الذين يحتاجون المساعدة",
  "Tell us about yourself" to "أخبرنا عن نفسك",
  "Create Password" to "إنشاء كلمة المرور",
  "Secure your account" to "تأمين حسابك",
  "Password *" to "كلمة المرور *",
  "Confirm Password *" to "تأكيد كلمة المرور *",
  "Re-enter your password" to "أعد إدخال كلمة المرور",
  "Password must contain:" to "يجب أن تحتوي كلمة المرور على:",
  "- At least 8 characters" to "- 8 أحرف على الأقل",
  "- A mix of letters and numbers" to "- مزيج من الحروف والأرقام",
  "- At least one special character" to "- رمز خاص واحد على الأقل",
  "What best describes your accessibility needs? *" to "ما الذي يصف احتياجات الوصول الخاصة بك؟ *",
  "Emergency Contact" to "جهة اتصال الطوارئ",
  "Someone we can reach in case of emergency" to "شخص يمكننا التواصل معه في حالة الطوارئ",
  "Full name is required." to "الاسم الكامل مطلوب.",
  "Email is required." to "البريد الإلكتروني مطلوب.",
  "Phone number is required." to "رقم الهاتف مطلوب.",
  "Location is required." to "الموقع مطلوب.",
  "Password must be at least 8 characters." to "يجب أن تكون كلمة المرور 8 أحرف على الأقل.",
  "Passwords do not match." to "كلمتا المرور غير متطابقتين.",
  "Please select or enter your accessibility needs." to "يرجى تحديد أو إدخال احتياجات الوصول الخاصة بك.",
  "Emergency contact name is required." to "اسم جهة اتصال الطوارئ مطلوب.",
  "Emergency contact phone is required." to "رقم هاتف جهة اتصال الطوارئ مطلوب.",

  // ── Register Volunteer ──
  "Become a Volunteer" to "كن متطوعاً",
  "Help others in your community" to "ساعد الآخرين في مجتمعك",
  "Identity Verification" to "التحقق من الهوية",
  "Required for volunteer approval" to "مطلوب للموافقة على التطوع",
  "Skills & Availability" to "المهارات والتوفر",
  "Help us match you with requests" to "ساعدنا في مطابقتك مع الطلبات",
  "Languages You Speak *" to "اللغات التي تتحدثها *",
  "Availability *" to "التوفر *",
  "Almost There!" to "أوشكت على الانتهاء!",
  "One last question" to "سؤال أخير",
  "Why do you want to volunteer with Athar? *" to "لماذا تريد التطوع مع أثر؟ *",
  "Share your motivation for helping others..." to "شارك دافعك لمساعدة الآخرين...",
  "Submit Application" to "تقديم الطلب",
  "National ID / Iqama number is required." to "رقم الهوية الوطنية / الإقامة مطلوب.",
  "Date of birth is required." to "تاريخ الميلاد مطلوب.",
  "Please select at least one language." to "يرجى اختيار لغة واحدة على الأقل.",
  "Please select at least one availability option." to "يرجى اختيار خيار توفر واحد على الأقل.",
  "Please write your volunteering motivation." to "يرجى كتابة دافع التطوع.",
  "Please select your accessibility needs." to "يرجى تحديد احتياجات الوصول الخاصة بك.",

  // ── Registration Field Labels (with asterisk) ──
  "Full Name *" to "الاسم الكامل *",
  "Email Address *" to "البريد الإلكتروني *",
  "Phone Number *" to "رقم الهاتف *",
  "City/Location *" to "المدينة/الموقع *",
  "Emergency Contact Name *" to "اسم جهة اتصال الطوارئ *",
  "Emergency Contact Phone *" to "رقم هاتف جهة اتصال الطوارئ *",
  "National ID / Iqama Number *" to "رقم الهوية الوطنية / الإقامة *",
  "Date of Birth *" to "تاريخ الميلاد *",

  // ── Registration Placeholders ──
  "your.email@example.com" to "بريدك@example.com",
  "+966 50 123 4567" to "+966 50 123 4567",
  "Enter your ID number" to "أدخل رقم الهوية",
  "YYYY-MM-DD" to "سنة-شهر-يوم",
  "Select date" to "اختر التاريخ",
  "Tap to upload ID document" to "اضغط لتحميل وثيقة الهوية",

  // ── Registration UI ──
  "Help us serve you better" to "ساعدنا لخدمتك بشكل أفضل",
  "Submitting..." to "جارٍ التقديم...",

  // ── Availability Options ──
  "Weekday mornings" to "صباح أيام الأسبوع",
  "Weekday afternoons" to "ظهر أيام الأسبوع",
  "Weekday evenings" to "مساء أيام الأسبوع",
  "Weekend mornings" to "صباح عطلة نهاية الأسبوع",
  "Weekend afternoons" to "ظهر عطلة نهاية الأسبوع",
  "Weekend evenings" to "مساء عطلة نهاية الأسبوع",
  "Flexible/On-demand" to "مرن/حسب الطلب",

  // ── Role Selection ──
  "Join Athar" to "انضم إلى أثر",
  "Already have an account? " to "لديك حساب بالفعل؟ ",

  // ── Add Place Report (extra) ──
  "Select Location" to "اختر الموقع",
  "Tap anywhere to drop pin" to "اضغط في أي مكان لوضع علامة",
  "Additional Comments (Optional)" to "تعليقات إضافية (اختياري)",

  // ── Map Screen (extra) ──
  "away from your location" to "بعيد عن موقعك",
  "Requesting as: " to "الطلب باسم: ",

  // ── Help & Support (extra) ──
  "How do I request volunteer assistance?" to "كيف أطلب مساعدة متطوع؟",
  "Tap the Request Help button on the Map screen, fill in your location and needs, and we'll match you with a nearby volunteer." to
    "اضغط على زر طلب المساعدة في شاشة الخريطة، واملأ موقعك واحتياجاتك، وسنطابقك مع متطوع قريب.",
  "How do I become a volunteer?" to "كيف أصبح متطوعاً؟",
  "Contact our support team to switch your account to volunteer mode. You'll need to complete verification before you can accept requests." to
    "تواصل مع فريق الدعم لتحويل حسابك إلى وضع المتطوع. ستحتاج لإكمال التحقق قبل قبول الطلبات.",
  "How is my data protected?" to "كيف تتم حماية بياناتي؟",
  "All your data is encrypted and stored securely. We never share your personal information without your consent." to
    "جميع بياناتك مشفرة ومخزنة بأمان. لا نشارك معلوماتك الشخصية أبداً دون موافقتك.",
  "Can I rate locations and volunteers?" to "هل يمكنني تقييم المواقع والمتطوعين؟",
  "Yes! After visiting a location or receiving help, you can rate and review to help others in the community." to
    "نعم! بعد زيارة موقع أو تلقي المساعدة، يمكنك التقييم والمراجعة لمساعدة الآخرين في المجتمع.",
  "What if no volunteer accepts my request?" to "ماذا لو لم يقبل أي متطوع طلبي؟",
  "If no volunteer is available within 3 minutes, we'll expand the search radius and notify more volunteers." to
    "إذا لم يتوفر متطوع خلال 3 دقائق، سنوسع نطاق البحث ونبلغ المزيد من المتطوعين.",
  "Send us a message" to "أرسل لنا رسالة",
  "Call Support Hotline" to "اتصل بخط الدعم",
  "Detailed feature guides" to "أدلة مفصلة للميزات",
  "What do you need help with?" to "بماذا تحتاج المساعدة؟",
  "Describe your issue or question in detail..." to "صف مشكلتك أو سؤالك بالتفصيل...",
  "Search documentation..." to "البحث في التوثيق...",
  "Search tutorials..." to "البحث في الدروس...",
  "\u00A9 2024 Athar. All rights reserved." to "\u00A9 2024 أثر. جميع الحقوق محفوظة.",
  "(c) 2024 Athar. All rights reserved." to "\u00A9 2024 أثر. جميع الحقوق محفوظة.",

  // ── Help Request Form (extra) ──
  "e.g., Central Mall entrance" to "مثال: مدخل المول المركزي",
  "e.g., Central Mall - Level 2, Store 45" to "مثال: المول المركزي - الطابق 2، محل 45",
  "Any additional information that might help the volunteer..." to "أي معلومات إضافية قد تساعد المتطوع...",

  // ── Selected (content description) ──
  "Selected" to "محدد",

  // ── SimpleCameraTranslator (extra) ──
  "LIVE" to "مباشر",
  "READY" to "جاهز",
  "Enable Camera" to "تفعيل الكاميرا",
  "1 hand" to "يد واحدة",
  "Volunteer Sentence" to "جملة المتطوع",
  "Interpreting the live sign sequence..." to "جارٍ تفسير تسلسل الإشارات المباشر...",
  "Arabic sentence" to "الجملة العربية",
  "English translation" to "الترجمة الإنجليزية",
  "Current Detection" to "الكشف الحالي",
  "No confirmed gesture yet." to "لا توجد إيماءة مؤكدة بعد.",
  "Transcript" to "النص",
  "English transcript" to "النص الإنجليزي",
  "Arabic transcript" to "النص العربي",
  "Supported Gestures" to "الإيماءات المدعومة",
  "Current Scope" to "النطاق الحالي",
  "Tap start detection to begin live recognition." to "اضغط بدء الكشف لبدء التعرف المباشر.",
  "Show one supported gesture with one hand." to "اعرض إيماءة مدعومة واحدة بيد واحدة.",
  "Transcript cleared. Start detection to analyze supported gestures." to
    "تم مسح النص. ابدأ الكشف لتحليل الإيماءات المدعومة.",
  "Allow camera access to start live recognition." to "اسمح بالوصول إلى الكاميرا لبدء التعرف المباشر."
)

private val dynamicPatterns = listOf(
  DynamicPattern(Regex("^Member since (.+)$")) { match ->
    "عضو منذ ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Accessibility needs: (.+)$")) { match ->
    "احتياجات الوصول: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Created: (.+)$")) { match ->
    "تم الإنشاء: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Last seen: (.+)$")) { match ->
    "آخر ظهور: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Last changed (.+)$")) { match ->
    "آخر تغيير: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Available balance: (.+)$")) { match ->
    "الرصيد المتاح: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Welcome back, (.+)$")) { match ->
    "مرحباً بعودتك، ${match.groupValues[1]}"
  },
  DynamicPattern(Regex("^Location: (.+)$")) { match ->
    "الموقع: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Help Type: (.+)$")) { match ->
    "نوع المساعدة: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Time: (.+)$")) { match ->
    "الوقت: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^User Type: (.+)$")) { match ->
    "نوع المستخدم: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Rating for: (.+)$")) { match ->
    "التقييم لـ: ${match.groupValues[1]}"
  },
  DynamicPattern(Regex("^Rating: (.+)$")) { match ->
    "التقييم: ${translateInline(match.groupValues[1])}"
  },
  DynamicPattern(Regex("^Sentence: (.+)$")) { match ->
    "الجملة: ${match.groupValues[1]}"
  },
  DynamicPattern(Regex("^Payment status: (.+)\\.$")) { match ->
    "حالة الدفع: ${translateInline(match.groupValues[1])}."
  },
  DynamicPattern(Regex("^Pay (.+) EGP with Paymob$")) { match ->
    "ادفع ${match.groupValues[1]} جنيه مصري عبر Paymob"
  },
  DynamicPattern(Regex("^Send Request .* (.+) EGP$")) { match ->
    "إرسال الطلب - ${match.groupValues[1]} جنيه مصري"
  },
  DynamicPattern(Regex("^No results found for \"(.+)\"\\.$")) { match ->
    "لم يتم العثور على نتائج لـ \"${match.groupValues[1]}\"."
  },
  DynamicPattern(Regex("^Show All (\\d+) Reviews$")) { match ->
    "عرض كل التقييمات (${match.groupValues[1]})"
  },
  DynamicPattern(Regex("^Found (\\d+) sections?$")) { match ->
    "تم العثور على ${match.groupValues[1]} قسم"
  },
  DynamicPattern(Regex("^Found (\\d+) tutorials?$")) { match ->
    "تم العثور على ${match.groupValues[1]} درس"
  },
  DynamicPattern(Regex("^Rate (\\d+) stars?$")) { match ->
    "قيّم بـ ${match.groupValues[1]} نجمة"
  },
  DynamicPattern(Regex("^You're in the top (\\d+)% of Athar volunteers$")) { match ->
    "أنت ضمن أفضل ${match.groupValues[1]}% من متطوعي أثر"
  },
  DynamicPattern(Regex("^Requested (.+) [•·] (.+) away$")) { match ->
    "تم الطلب ${translateInline(match.groupValues[1])} • على بعد ${translateInline(match.groupValues[2])}"
  },
  DynamicPattern(Regex("^(\\d+) hours? × (\\d+) EGP$")) { match ->
    "${match.groupValues[1]} ساعة × ${match.groupValues[2]} جنيه مصري"
  },
  DynamicPattern(Regex("^\\((\\d+)h × (\\d+) EGP\\)$")) { match ->
    "(${match.groupValues[1]} س × ${match.groupValues[2]} جنيه مصري)"
  },
  DynamicPattern(Regex("^(\\d+)h × (\\d+) EGP$")) { match ->
    "${match.groupValues[1]} س × ${match.groupValues[2]} جنيه مصري"
  },
  DynamicPattern(Regex("^(.+) - (\\d+)hrs?$")) { match ->
    "${match.groupValues[1]} - ${match.groupValues[2]} س"
  },
  DynamicPattern(Regex("^(\\d+) hours?$")) { match ->
    val n = match.groupValues[1].toIntOrNull() ?: 1
    "${match.groupValues[1]} ${if (n == 1) "ساعة" else "ساعات"}"
  },
  DynamicPattern(Regex("^(.+) EGP/hr x (\\d+)hr$")) { match ->
    "${match.groupValues[1]} جنيه مصري/ساعة × ${match.groupValues[2]} س"
  },
  DynamicPattern(Regex("^(\\d+)hr(s?)$")) { match ->
    "${match.groupValues[1]} س"
  },
  DynamicPattern(Regex("^(.+) EGP/hr$")) { match ->
    "${match.groupValues[1]} جنيه مصري/ساعة"
  },
  DynamicPattern(Regex("^\\+(.+) EGP$")) { match ->
    "+${match.groupValues[1]} جنيه مصري"
  },
  DynamicPattern(Regex("^-(.+) EGP$")) { match ->
    "-${match.groupValues[1]} جنيه مصري"
  },
  DynamicPattern(Regex("^(.+) EGP$")) { match ->
    "${match.groupValues[1]} جنيه مصري"
  },
  DynamicPattern(Regex("^(.+) km$")) { match ->
    "${match.groupValues[1]} كم"
  },
  DynamicPattern(Regex("^(.+) m$")) { match ->
    "${match.groupValues[1]} م"
  },
  DynamicPattern(Regex("^(\\d+) mins? ago$")) { match ->
    "منذ ${match.groupValues[1]} دقيقة"
  },
  DynamicPattern(Regex("^(\\d+) hours? ago$")) { match ->
    "منذ ${match.groupValues[1]} ساعة"
  },
  DynamicPattern(Regex("^(\\d+) days$")) { match ->
    "${match.groupValues[1]} يوم"
  },
  DynamicPattern(Regex("^(\\d+) reviews$")) { match ->
    "${match.groupValues[1]} تقييم"
  },
  DynamicPattern(Regex("^(\\d+) ratings$")) { match ->
    "${match.groupValues[1]} تقييم"
  },
  DynamicPattern(Regex("^(\\d+) assists$")) { match ->
    "${match.groupValues[1]} مساعدة"
  },
  DynamicPattern(Regex("^(\\d+) topics$")) { match ->
    "${match.groupValues[1]} موضوع"
  },
  DynamicPattern(Regex("^(\\d+) articles?$")) { match ->
    "${match.groupValues[1]} مقال"
  },
  DynamicPattern(Regex("^(\\d+) issues?$")) { match ->
    "${match.groupValues[1]} مشكلة"
  },
  DynamicPattern(Regex("^(\\d+) Accessible Places? Nearby$")) { match ->
    "${match.groupValues[1]} مكان يسهل الوصول إليه بالقرب"
  },
  DynamicPattern(Regex("^(.+) away from your location$")) { match ->
    "${match.groupValues[1]} بعيداً عن موقعك"
  }
)

private val tokenTranslations = linkedMapOf(
  "January" to "يناير",
  "February" to "فبراير",
  "March" to "مارس",
  "April" to "أبريل",
  "May" to "مايو",
  "June" to "يونيو",
  "July" to "يوليو",
  "August" to "أغسطس",
  "September" to "سبتمبر",
  "October" to "أكتوبر",
  "November" to "نوفمبر",
  "December" to "ديسمبر",
  "Jan" to "يناير",
  "Feb" to "فبراير",
  "Mar" to "مارس",
  "Apr" to "أبريل",
  "Jun" to "يونيو",
  "Jul" to "يوليو",
  "Aug" to "أغسطس",
  "Sep" to "سبتمبر",
  "Oct" to "أكتوبر",
  "Nov" to "نوفمبر",
  "Dec" to "ديسمبر",
  "Saturday" to "السبت",
  "Sunday" to "الأحد",
  "Monday" to "الاثنين",
  "Tuesday" to "الثلاثاء",
  "Wednesday" to "الأربعاء",
  "Thursday" to "الخميس",
  "Friday" to "الجمعة",
  "Sat" to "السبت",
  "Sun" to "الأحد",
  "Mon" to "الاثنين",
  "Tue" to "الثلاثاء",
  "Wed" to "الأربعاء",
  "Thu" to "الخميس",
  "Fri" to "الجمعة",
  "EGP/hr" to "جنيه مصري/ساعة",
  "EGP" to "جنيه مصري",
  "km" to "كم",
  "mins ago" to "دقيقة مضت",
  "min ago" to "دقيقة مضت",
  "hours ago" to "ساعة مضت",
  "hour ago" to "ساعة مضت",
  "reviews" to "تقييمات",
  "ratings" to "تقييمات",
  "rating" to "تقييم",
  "topics" to "مواضيع",
  "articles" to "مقالات",
  "article" to "مقال",
  "issues" to "مشكلات",
  "issue" to "مشكلة",
  "days" to "أيام",
  "day" to "يوم",
  "hr" to "س",
  "hrs" to "س",
  "Bank Transfer" to "تحويل بنكي",
  "Paymob Wallet" to "محفظة Paymob"
)

private fun translateInline(rawText: String): String {
  overrideTranslations[rawText]?.let { return it }
  translationCache?.get(rawText)?.let {
    return it
      .replace("اثار", "أثر")
      .replace("Athar", "أثر")
  }
  var translated = rawText
  tokenTranslations.forEach { (source, target) ->
    translated = translated.replace(source, target, ignoreCase = false)
  }
  return translated
    .replace("اثار", "أثر")
    .replace("Athar", "أثر")
}

private fun looksLikeMachineValue(rawText: String): Boolean {
  return rawText.contains("@") ||
    rawText.startsWith("http") ||
    rawText.startsWith("www.") ||
    rawText.matches(Regex("^[+]?\\d[\\d\\s()-]*$"))
}

@Volatile
private var translationCache: Map<String, String>? = null

private fun loadArabicTranslations(context: Context): Map<String, String> {
  translationCache?.let { return it }
  synchronized(AppLocalizationLock) {
    translationCache?.let { return it }
    val translations = runCatching {
      val rawJson = context.assets.open("translations_en_ar.json").bufferedReader().use { it.readText() }
      val jsonObject = JSONObject(rawJson)
      buildMap {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          put(key, jsonObject.optString(key))
        }
      }
    }.getOrDefault(emptyMap())
    translationCache = translations
    return translations
  }
}

private object AppLocalizationLock
