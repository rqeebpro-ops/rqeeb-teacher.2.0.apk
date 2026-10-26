-- ==============================================================================
-- مشروع تطبيق رقيب – مساعد المعلم الذكي (Rqeeb Teacher)
-- مخطط قاعدة بيانات PostgreSQL الرسمية عبر Supabase
-- يتضمن الجداول، الفهارس، سياسات الأمان Row Level Security (RLS)، والدوال المحمية (RPC)
-- ==============================================================================

-- 1. تمكين امتدادات التشفير وتوليد المعرفات الفريدة
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 2. إنشاء الجداول الأساسية
-- ==============================================================================

-- جدول المعلمين (teachers)
CREATE TABLE IF NOT EXISTS public.teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL DEFAULT '',
    school_name TEXT NOT NULL,
    governorate TEXT NOT NULL DEFAULT 'أمانة العاصمة',
    district TEXT NOT NULL DEFAULT '',
    subject TEXT NOT NULL DEFAULT 'الرياضيات',
    education_stage TEXT NOT NULL DEFAULT 'مرحلة التعليم الأساسي',
    grade TEXT NOT NULL, -- مثال: '9/ إعدادي', '1/ أساسي'
    section TEXT NOT NULL DEFAULT '', -- أ، ب، ج، د، هـ، و، أخرى
    curriculum_edition TEXT NOT NULL DEFAULT 'طبعة صنعاء (وزارة التربية والتعليم - صنعاء)',
    academic_year TEXT NOT NULL DEFAULT '1446-1447هـ / 2024-2025م',
    request_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    is_activated BOOLEAN NOT NULL DEFAULT false,
    activation_code TEXT NOT NULL DEFAULT '',
    device_model TEXT NOT NULL DEFAULT '',
    review_notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول أكواد التفعيل الأحادية الاستخدام (activation_codes)
CREATE TABLE IF NOT EXISTS public.activation_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES public.teachers(id) ON DELETE CASCADE,
    code_hash TEXT NOT NULL,
    plain_code TEXT NOT NULL, -- رمز التفعيل المولد للمالك
    is_used BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '30 days'),
    used_at TIMESTAMPTZ,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول إعدادات وهوية المالك (owner_config)
CREATE TABLE IF NOT EXISTS public.owner_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID REFERENCES auth.users(id),
    owner_email TEXT NOT NULL DEFAULT 'abdulrqeebfarag2018@gmail.com',
    owner_phone TEXT NOT NULL DEFAULT '+967785502919',
    settings JSONB NOT NULL DEFAULT '{
        "auto_approve": false,
        "notify_on_new_request": true,
        "default_activation_duration_days": 30,
        "app_version": "2.1"
    }'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول سجل العمليات الإدارية والرقابة (audit_logs)
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID REFERENCES auth.users(id),
    teacher_id UUID REFERENCES public.teachers(id) ON DELETE SET NULL,
    teacher_name TEXT NOT NULL DEFAULT '',
    action TEXT NOT NULL, -- APPROVE, REJECT, SUSPEND, REACTIVATE, CODE_GENERATION
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول إشعارات المعلمين (teacher_notifications)
CREATE TABLE IF NOT EXISTS public.teacher_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES public.teachers(id) ON DELETE CASCADE,
    notification_type TEXT NOT NULL DEFAULT 'SYSTEM',
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ==============================================================================
-- 3. إنشاء الفهارس لتحسين سرعة الاستعلامات والبحث
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_teachers_status ON public.teachers(request_status);
CREATE INDEX IF NOT EXISTS idx_teachers_email ON public.teachers(email);
CREATE INDEX IF NOT EXISTS idx_teachers_phone ON public.teachers(phone);
CREATE INDEX IF NOT EXISTS idx_teachers_created ON public.teachers(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_codes_teacher ON public.activation_codes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_codes_plain ON public.activation_codes(plain_code);
CREATE INDEX IF NOT EXISTS idx_audit_created ON public.audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifs_teacher ON public.teacher_notifications(teacher_id, is_read);

-- ==============================================================================
-- 4. دوال التحديث التلقائي لوقت التعديل (updated_at)
-- ==============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_teachers_updated_at ON public.teachers;
CREATE TRIGGER trigger_teachers_updated_at
    BEFORE UPDATE ON public.teachers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==============================================================================
-- 5. إعداد الأمان وسياسات الصفوف Row Level Security (RLS)
-- ==============================================================================
ALTER TABLE public.teachers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activation_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.owner_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teacher_notifications ENABLE ROW LEVEL SECURITY;

-- دالة للتحقق من هوية المالك الأساسي (البريد الرسمي للمالك)
CREATE OR REPLACE FUNCTION public.is_owner()
RETURNS BOOLEAN AS $$
DECLARE
    current_email TEXT;
BEGIN
    current_email := auth.jwt() ->> 'email';
    IF current_email = 'abdulrqeebfarag2018@gmail.com' THEN
        RETURN TRUE;
    END IF;
    -- فحص ما إذا كان معرّف المستخدم مسجلاً في جدول owner_config
    IF EXISTS (SELECT 1 FROM public.owner_config WHERE owner_user_id = auth.uid()) THEN
        RETURN TRUE;
    END IF;
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- سياسات جدول المعلمين (teachers)
-- 1) السماح لأي معلم بإنشاء طلب تسجيل جديد (حتى بدون تسجيل دخول مسبق)
DROP POLICY IF EXISTS "Allow teacher registration" ON public.teachers;
CREATE POLICY "Allow teacher registration"
    ON public.teachers FOR INSERT
    WITH CHECK (
        request_status = 'PENDING' AND
        is_activated = false
    );

-- 2) السماح بقراءة السجلات:
-- - المالك يقرأ جميع السجلات
-- - أو المعلم يقرأ سجله الخاص عبر بريده أو معرّفه
DROP POLICY IF EXISTS "Allow read teachers" ON public.teachers;
CREATE POLICY "Allow read teachers"
    ON public.teachers FOR SELECT
    USING (
        public.is_owner()
        OR auth.uid() = auth_user_id
        OR auth.jwt() ->> 'email' = email
        OR auth.role() = 'anon' -- للسماح بالتحقق الأولي من حالة المعلم برقم الهاتف أو البريد
    );

-- 3) السماح بتحديث السجل:
-- - المالك يملك حق التعديل الكامل
-- - المعلم يستطيع تحديث بياناته الشخصية (مثل الاسم أو المدرسة) فقط إذا كان حسابه مفعلًا وبدون تغيير الحالة
DROP POLICY IF EXISTS "Allow update teachers" ON public.teachers;
CREATE POLICY "Allow update teachers"
    ON public.teachers FOR UPDATE
    USING (
        public.is_owner()
        OR (
            (auth.uid() = auth_user_id OR auth.jwt() ->> 'email' = email)
            AND request_status = 'APPROVED'
        )
    );

-- سياسات جدول أكواد التفعيل (activation_codes)
-- حظر الوصول العام، والسماح للمالك فقط بالقراءة والإنشاء
DROP POLICY IF EXISTS "Owner access on activation codes" ON public.activation_codes;
CREATE POLICY "Owner access on activation codes"
    ON public.activation_codes FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- سياسات جدول سجلات التدقيق (audit_logs)
DROP POLICY IF EXISTS "Owner access on audit logs" ON public.audit_logs;
CREATE POLICY "Owner access on audit logs"
    ON public.audit_logs FOR SELECT
    USING (public.is_owner() OR auth.role() = 'service_role');

DROP POLICY IF EXISTS "Insert audit logs" ON public.audit_logs;
CREATE POLICY "Insert audit logs"
    ON public.audit_logs FOR INSERT
    WITH CHECK (public.is_owner() OR auth.role() = 'service_role');

-- سياسات جدول إعدادات المالك (owner_config)
DROP POLICY IF EXISTS "Owner access on owner config" ON public.owner_config;
CREATE POLICY "Owner access on owner config"
    ON public.owner_config FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- سياسات جدول إشعارات المعلمين (teacher_notifications)
DROP POLICY IF EXISTS "Teacher view notifications" ON public.teacher_notifications;
CREATE POLICY "Teacher view notifications"
    ON public.teacher_notifications FOR SELECT
    USING (
        public.is_owner()
        OR teacher_id IN (
            SELECT id FROM public.teachers WHERE auth_user_id = auth.uid() OR email = auth.jwt() ->> 'email'
        )
    );

-- ==============================================================================
-- 6. الدوال المحمية على الخادم (PostgreSQL Functions / RPC)
-- هذه الدوال تعمل بصلاحيات SECURITY DEFINER لضمان معالجة العمليات الحساسة بأمان
-- ==============================================================================

-- 1) دالة الموافقة على طلب المعلم وتوليد كود التفعيل
CREATE OR REPLACE FUNCTION public.rpc_approve_teacher(
    p_teacher_id UUID,
    p_custom_code TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_code TEXT;
    v_teacher RECORD;
BEGIN
    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    -- توليد كود التفعيل
    IF p_custom_code IS NOT NULL AND length(trim(p_custom_code)) > 0 THEN
        v_code := trim(p_custom_code);
    ELSE
        v_code := 'RQ-' || (1000 + floor(random() * 9000))::TEXT;
    END IF;

    -- تحديث حالة المعلم
    UPDATE public.teachers
    SET request_status = 'APPROVED',
        is_activated = true,
        activation_code = v_code,
        review_notes = 'تمت الموافقة وتفعيل الحساب من لوحة المالك',
        updated_at = now()
    WHERE id = p_teacher_id;

    -- حفظ الكود في جدول الأكواد
    INSERT INTO public.activation_codes (teacher_id, code_hash, plain_code, is_used, created_by)
    VALUES (p_teacher_id, crypt(v_code, gen_salt('bf')), v_code, true, auth.uid());

    -- تسجيل العملية في سجل التدقيق
    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'APPROVE',
        jsonb_build_object('code', v_code, 'school', v_teacher.school_name, 'phone', v_teacher.phone)
    );

    -- إرسال إشعار داخلي للمعلم
    INSERT INTO public.teacher_notifications (teacher_id, notification_type, title, message)
    VALUES (
        p_teacher_id,
        'ACTIVATION',
        'تم تفعيل حسابك بنجاح!',
        'مرحباً بك يا أستاذنا في رقيب. تم اعتماد حسابك رسميًا، كود التفعيل: ' || v_code
    );

    RETURN jsonb_build_object('success', true, 'code', v_code, 'message', 'تمت الموافقة وتفعيل الحساب بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2) دالة رفض طلب المعلم
CREATE OR REPLACE FUNCTION public.rpc_reject_teacher(
    p_teacher_id UUID,
    p_reason TEXT DEFAULT 'طلب غير مستوفٍ للشروط'
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
BEGIN
    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    UPDATE public.teachers
    SET request_status = 'REJECTED',
        is_activated = false,
        review_notes = p_reason,
        updated_at = now()
    WHERE id = p_teacher_id;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'REJECT',
        jsonb_build_object('reason', p_reason, 'phone', v_teacher.phone)
    );

    INSERT INTO public.teacher_notifications (teacher_id, notification_type, title, message)
    VALUES (
        p_teacher_id,
        'REJECTION',
        'حالة طلب التسجيل',
        'نعتذر، لم تتم الموافقة على الطلب للأسباب التالية: ' || p_reason
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم رفض الطلب بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3) دالة تعليق الحساب مؤقتًا
CREATE OR REPLACE FUNCTION public.rpc_suspend_teacher(
    p_teacher_id UUID,
    p_reason TEXT DEFAULT 'إيقاف إداري مؤقت'
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
BEGIN
    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    UPDATE public.teachers
    SET request_status = 'SUSPENDED',
        is_activated = false,
        review_notes = p_reason,
        updated_at = now()
    WHERE id = p_teacher_id;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'SUSPEND',
        jsonb_build_object('reason', p_reason)
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم تعليق الحساب');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4) دالة إعادة تفعيل الحساب المعلق
CREATE OR REPLACE FUNCTION public.rpc_reactivate_teacher(
    p_teacher_id UUID
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
BEGIN
    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    UPDATE public.teachers
    SET request_status = 'APPROVED',
        is_activated = true,
        review_notes = 'تمت إعادة التفعيل بواسطة المالك',
        updated_at = now()
    WHERE id = p_teacher_id;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'REACTIVATE',
        jsonb_build_object('teacher', v_teacher.full_name)
    );

    RETURN jsonb_build_object('success', true, 'message', 'تمت إعادة التفعيل بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5) دالة التفعيل بكود الأمان من جانب المعلم
CREATE OR REPLACE FUNCTION public.rpc_activate_with_code(
    p_identifier TEXT,
    p_code TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
    v_clean_id TEXT;
    v_clean_code TEXT;
BEGIN
    v_clean_id := trim(p_identifier);
    v_clean_code := trim(p_code);

    SELECT * INTO v_teacher
    FROM public.teachers
    WHERE (email = v_clean_id OR phone = v_clean_id)
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'الحساب غير مسجل في النظام');
    END IF;

    -- التحقق من تطابق الكود المخزن أو وجود كود في جدول activation_codes
    IF (v_teacher.activation_code IS NOT NULL AND lower(v_teacher.activation_code) = lower(v_clean_code))
       OR EXISTS (
           SELECT 1 FROM public.activation_codes
           WHERE teacher_id = v_teacher.id
             AND lower(plain_code) = lower(v_clean_code)
             AND is_used = false
             AND expires_at > now()
       ) THEN

        UPDATE public.teachers
        SET request_status = 'APPROVED',
            is_activated = true,
            updated_at = now()
        WHERE id = v_teacher.id;

        UPDATE public.activation_codes
        SET is_used = true,
            used_at = now()
        WHERE teacher_id = v_teacher.id AND lower(plain_code) = lower(v_clean_code);

        RETURN jsonb_build_object(
            'success', true,
            'message', 'تم تفعيل حسابك بنجاح! يمكنك الآن استخدام تطبيق رقيب كاملاً',
            'teacher', row_to_json(v_teacher)
        );
    ELSE
        RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل غير صحيح أو منتهي الصلاحية');
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ==============================================================================
-- 7. تهيئة سجل المالك الافتراضي في owner_config
-- ==============================================================================
INSERT INTO public.owner_config (owner_email, owner_phone, settings)
VALUES (
    'abdulrqeebfarag2018@gmail.com',
    '+967785502919',
    '{
        "auto_approve": false,
        "notify_on_new_request": true,
        "app_name": "رقيب المعلم",
        "owner_device": "Realme"
    }'::jsonb
)
ON CONFLICT DO NOTHING;
