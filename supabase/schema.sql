-- ==============================================================================
-- مشروع تطبيق رقيب – مساعد المعلم الذكي (Rqeeb Teacher v2.1)
-- المخطط النهائي لقاعدة بيانات PostgreSQL عبر Supabase
-- تأمين صارم: Row Level Security (RLS) + حماية العمليات الإدارية + منع تكرار الأكواد
-- ==============================================================================

-- 1. تمكين امتدادات التشفير وتوليد المعرفات الفريدة
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 2. إنشاء الجداول الأساسية الخمسة
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
    grade TEXT NOT NULL,
    section TEXT NOT NULL DEFAULT '',
    curriculum_edition TEXT NOT NULL DEFAULT 'طبعة صنعاء (وزارة التربية والتعليم - صنعاء)',
    academic_year TEXT NOT NULL DEFAULT '1446-1447هـ / 2024-2025م',
    request_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    is_activated BOOLEAN NOT NULL DEFAULT false,
    device_model TEXT NOT NULL DEFAULT '',
    review_notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول أكواد التفعيل الأحادية الاستخدام (activation_codes)
CREATE TABLE IF NOT EXISTS public.activation_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID REFERENCES public.teachers(id) ON DELETE CASCADE,
    code_hash TEXT NOT NULL,
    plain_code TEXT NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT false,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_reason TEXT DEFAULT '',
    revoked_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '30 days'),
    used_at TIMESTAMPTZ,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- تحديث الحقول إذا كان الجدول منشأ مسبقاً
ALTER TABLE public.activation_codes ALTER COLUMN teacher_id DROP NOT NULL;
ALTER TABLE public.activation_codes ADD COLUMN IF NOT EXISTS is_revoked BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.activation_codes ADD COLUMN IF NOT EXISTS revoked_reason TEXT DEFAULT '';
ALTER TABLE public.activation_codes ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
ALTER TABLE public.activation_codes ADD COLUMN IF NOT EXISTS assigned_name TEXT DEFAULT '';
ALTER TABLE public.activation_codes ADD COLUMN IF NOT EXISTS assigned_phone TEXT DEFAULT '';
ALTER TABLE public.activation_codes ADD COLUMN IF NOT EXISTS duration_days INT DEFAULT 365;
CREATE UNIQUE INDEX IF NOT EXISTS idx_codes_plain_unique ON public.activation_codes(plain_code);

-- جدول إعدادات وهوية المالك (owner_config)
CREATE TABLE IF NOT EXISTS public.owner_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID REFERENCES auth.users(id) UNIQUE,
    owner_email TEXT NOT NULL UNIQUE,
    owner_phone TEXT NOT NULL DEFAULT '+967785502919',
    owner_pin_hash TEXT,
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
    action TEXT NOT NULL,
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
-- 3. الفهارس لتحسين سرعة الاستعلامات والبحث
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_teachers_status ON public.teachers(request_status);
CREATE INDEX IF NOT EXISTS idx_teachers_email ON public.teachers(email);
CREATE INDEX IF NOT EXISTS idx_teachers_phone ON public.teachers(phone);
CREATE INDEX IF NOT EXISTS idx_teachers_created ON public.teachers(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_codes_teacher ON public.activation_codes(teacher_id);
CREATE INDEX IF NOT EXISTS idx_codes_plain ON public.activation_codes(plain_code);
CREATE INDEX IF NOT EXISTS idx_codes_active_check ON public.activation_codes(teacher_id, plain_code, is_used);
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
-- 5. الأمان الصارم وسياسات الصفوف Row Level Security (RLS)
-- ==============================================================================
ALTER TABLE public.teachers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activation_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.owner_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teacher_notifications ENABLE ROW LEVEL SECURITY;

-- دالة خادم موثقة للتحقق من هوية المالك دون الاعتماد على البريد وحده
CREATE OR REPLACE FUNCTION public.is_owner()
RETURNS BOOLEAN AS $$
DECLARE
    v_uid UUID;
BEGIN
    -- 1. دور الخدمة المباشر من لوحة Supabase (Service Role)
    IF auth.role() = 'service_role' THEN
        RETURN TRUE;
    END IF;

    v_uid := auth.uid();
    IF v_uid IS NULL THEN
        RETURN FALSE;
    END IF;

    -- 2. التحقق من تطابق معرف المستخدم مع الحساب المعتمد رسمياً في owner_config
    IF EXISTS (
        SELECT 1 FROM public.owner_config
        WHERE owner_user_id = v_uid
    ) THEN
        RETURN TRUE;
    END IF;

    -- 3. التحقق من وجود دور المالك الموثق داخل app_metadata الموقعة رقمياً
    IF (auth.jwt() -> 'app_metadata' ->> 'role') = 'owner'
       AND (auth.jwt() -> 'app_metadata' ->> 'verified')::BOOLEAN IS TRUE THEN
        RETURN TRUE;
    END IF;

    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- دالة مركزية للتحقق من هوية المالك أو تطابق رمز المرور PIN مع التهيئة الذاتية الآمنة
CREATE OR REPLACE FUNCTION public.check_owner_auth(p_owner_pin TEXT DEFAULT NULL)
RETURNS BOOLEAN AS $$
DECLARE
    v_cfg RECORD;
    v_clean_pin TEXT;
BEGIN
    -- 1. فحص هوية المالك عبر JWT أو دور الخدمة
    IF public.is_owner() THEN
        RETURN TRUE;
    END IF;

    v_clean_pin := trim(COALESCE(p_owner_pin, ''));
    IF length(v_clean_pin) = 0 THEN
        RETURN FALSE;
    END IF;

    SELECT * INTO v_cfg FROM public.owner_config WHERE owner_email = 'abdulrqeebfarag2018@gmail.com' LIMIT 1;
    IF NOT FOUND THEN
        -- تهيئة تلقائية أولية لحساب المالك المعتمد بالـ PIN المُدخل مشفراً بـ bcrypt
        INSERT INTO public.owner_config (owner_email, owner_pin_hash)
        VALUES ('abdulrqeebfarag2018@gmail.com', crypt(v_clean_pin, gen_salt('bf')));
        RETURN TRUE;
    END IF;

    IF v_cfg.owner_pin_hash IS NULL OR length(trim(v_cfg.owner_pin_hash)) = 0 THEN
        UPDATE public.owner_config
        SET owner_pin_hash = crypt(v_clean_pin, gen_salt('bf')),
            updated_at = now()
        WHERE id = v_cfg.id;
        RETURN TRUE;
    END IF;

    RETURN (v_cfg.owner_pin_hash = crypt(v_clean_pin, v_cfg.owner_pin_hash));
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.check_owner_auth(TEXT) TO anon, authenticated, service_role;

-- ------------------------------------------------------------------------------
-- سياسات جدول المعلمين (teachers)
-- ------------------------------------------------------------------------------

-- 1) السماح بتسجيل طلب جديد فقط بحالة PENDING وغير مفعل
DROP POLICY IF EXISTS "Allow teacher registration" ON public.teachers;
CREATE POLICY "Allow teacher registration"
    ON public.teachers FOR INSERT
    WITH CHECK (
        request_status = 'PENDING'
        AND is_activated = false
    );

-- 2) حظر القراءة العامة لبيانات المعلمين:
-- يمنع للعامة (anon) قراءة جدول المعلمين نهائياً.
-- المالك الموثق يقرأ جميع السجلات.
-- المعلم المسجل يقرأ سجله الخاص فقط.
DROP POLICY IF EXISTS "Allow read teachers" ON public.teachers;
CREATE POLICY "Allow read teachers"
    ON public.teachers FOR SELECT
    USING (
        public.is_owner()
        OR auth.uid() = auth_user_id
        OR auth.role() = 'service_role'
    );

-- 3) تحديث بيانات المعلم:
-- المالك يملك حق التعديل الكامل.
-- المعلم المسجل يحدّث بياناته الشخصية فقط إذا كان حسابه مفعلاً مسبقاً وبدون تغيير الحالة.
DROP POLICY IF EXISTS "Allow update teachers" ON public.teachers;
CREATE POLICY "Allow update teachers"
    ON public.teachers FOR UPDATE
    USING (
        public.is_owner()
        OR (
            auth.uid() = auth_user_id
            AND request_status = 'APPROVED'
            AND is_activated = true
        )
    );

-- ------------------------------------------------------------------------------
-- سياسات جدول أكواد التفعيل (activation_codes)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Owner access on activation codes" ON public.activation_codes;
CREATE POLICY "Owner access on activation codes"
    ON public.activation_codes FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- ------------------------------------------------------------------------------
-- سياسات جدول سجلات التدقيق (audit_logs)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Owner access on audit logs" ON public.audit_logs;
CREATE POLICY "Owner access on audit logs"
    ON public.audit_logs FOR SELECT
    USING (public.is_owner() OR auth.role() = 'service_role');

DROP POLICY IF EXISTS "Insert audit logs" ON public.audit_logs;
CREATE POLICY "Insert audit logs"
    ON public.audit_logs FOR INSERT
    WITH CHECK (public.is_owner() OR auth.role() = 'service_role');

-- ------------------------------------------------------------------------------
-- سياسات جدول إعدادات المالك (owner_config)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Owner access on owner config" ON public.owner_config;
CREATE POLICY "Owner access on owner config"
    ON public.owner_config FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- ------------------------------------------------------------------------------
-- سياسات جدول إشعارات المعلمين (teacher_notifications)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Teacher view notifications" ON public.teacher_notifications;
CREATE POLICY "Teacher view notifications"
    ON public.teacher_notifications FOR SELECT
    USING (
        public.is_owner()
        OR teacher_id IN (
            SELECT id FROM public.teachers WHERE auth_user_id = auth.uid()
        )
    );

-- سلب الصلاحيات العامة المباشرة
REVOKE ALL ON public.activation_codes FROM anon;
REVOKE ALL ON public.owner_config FROM anon;
REVOKE ALL ON public.audit_logs FROM anon;

-- ==============================================================================
-- 6. دوال التحقق وإدارة المالك بأمان (بدون كلمات مرور افتراضية)
-- ==============================================================================

-- التحقق من هوية المالك أو تعيين كلمة المرور الأولية الآمنة
CREATE OR REPLACE FUNCTION public.rpc_verify_owner(
    p_password TEXT,
    p_email TEXT DEFAULT ''
)
RETURNS JSONB AS $$
DECLARE
    v_cfg RECORD;
    v_valid BOOLEAN := false;
BEGIN
    IF p_password IS NULL OR length(trim(p_password)) < 6 THEN
        RETURN jsonb_build_object('verified', false, 'error', 'كلمة المرور يجب ألا تقل عن 6 خانات');
    END IF;

    SELECT * INTO v_cfg FROM public.owner_config WHERE owner_email = 'abdulrqeebfarag2018@gmail.com' LIMIT 1;

    IF NOT FOUND THEN
        -- تهيئة الحساب الأولي بالـ PIN الذي يحدده المالك بنفسه مشفراً بـ bcrypt
        INSERT INTO public.owner_config (owner_email, owner_pin_hash)
        VALUES ('abdulrqeebfarag2018@gmail.com', crypt(trim(p_password), gen_salt('bf')))
        RETURNING * INTO v_cfg;
        v_valid := true;
    ELSE
        IF v_cfg.owner_pin_hash IS NOT NULL AND v_cfg.owner_pin_hash <> '' THEN
            v_valid := (v_cfg.owner_pin_hash = crypt(trim(p_password), v_cfg.owner_pin_hash));
        ELSE
            -- إذا كان الـ PIN غير محدد بعد، يتم تعيينه بأول قيمة يدخلها المالك
            UPDATE public.owner_config
            SET owner_pin_hash = crypt(trim(p_password), gen_salt('bf')),
                updated_at = now()
            WHERE id = v_cfg.id;
            v_valid := true;
        END IF;
    END IF;

    -- إذا كان المالك مسجلاً بحساب Supabase Auth يتم ربط المعرف تلقائياً
    IF v_valid AND auth.uid() IS NOT NULL THEN
        UPDATE public.owner_config
        SET owner_user_id = auth.uid(),
            updated_at = now()
        WHERE id = v_cfg.id;
    END IF;

    RETURN jsonb_build_object(
        'verified', v_valid,
        'owner_email', v_cfg.owner_email,
        'owner_phone', v_cfg.owner_phone
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_verify_owner(TEXT, TEXT) TO anon, authenticated, service_role;

-- ربط حساب المالك المعتمد عبر Supabase Auth
CREATE OR REPLACE FUNCTION public.rpc_claim_owner_role()
RETURNS JSONB AS $$
DECLARE
    v_uid UUID;
    v_email TEXT;
BEGIN
    v_uid := auth.uid();
    v_email := auth.jwt() ->> 'email';

    IF v_uid IS NULL OR v_email IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'يجب تسجيل الدخول بحساب المالك المعتمد أولاً');
    END IF;

    IF lower(trim(v_email)) <> 'abdulrqeebfarag2018@gmail.com' THEN
        RETURN jsonb_build_object('success', false, 'error', 'هذا الحساب غير مصرح له بطلب صلاحية المالك');
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.owner_config
        WHERE owner_user_id IS NOT NULL AND owner_user_id <> v_uid
    ) THEN
        RETURN jsonb_build_object('success', false, 'error', 'تم تعيين معرّف المالك مسبقاً ولا يمكن ربط حساب آخر');
    END IF;

    INSERT INTO public.owner_config (owner_user_id, owner_email, owner_phone)
    VALUES (v_uid, v_email, '+967785502919')
    ON CONFLICT (owner_email) DO UPDATE
    SET owner_user_id = v_uid,
        updated_at = now();

    RETURN jsonb_build_object('success', true, 'message', 'تم ربط حساب المالك بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_claim_owner_role() TO authenticated, service_role;

-- ==============================================================================
-- 7. الدوال الإدارية المحمية (الموافقة، الرفض، التعليق، استعراض المعلمين)
-- ==============================================================================

-- استعراض كافة المعلمين للمالك المعتمد حصراً
CREATE OR REPLACE FUNCTION public.rpc_get_all_teachers_for_owner(
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_array();
    END IF;

    RETURN (
        SELECT COALESCE(jsonb_agg(row_to_json(t) ORDER BY t.created_at DESC), '[]'::jsonb)
        FROM public.teachers t
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_get_all_teachers_for_owner(TEXT) TO anon, authenticated, service_role;

-- الموافقة على طلب المعلم وتوليد كود التفعيل
CREATE OR REPLACE FUNCTION public.rpc_approve_teacher(
    p_teacher_id UUID,
    p_custom_code TEXT DEFAULT NULL,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_code TEXT;
    v_teacher RECORD;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: هذه العملية محصورة بمالك النظام المعتمد فقط');
    END IF;

    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    IF p_custom_code IS NOT NULL AND length(trim(p_custom_code)) > 0 THEN
        v_code := upper(trim(p_custom_code));
    ELSE
        v_code := 'RQ-' || (1000 + floor(random() * 9000))::TEXT || '-' || chr(65 + floor(random() * 26)::INT);
    END IF;

    -- تحديث حالة المعلم إلى APPROVED وتظل is_activated = false حتى يُدخل الكود بنفسه
    UPDATE public.teachers
    SET request_status = 'APPROVED',
        is_activated = false,
        review_notes = 'تمت الموافقة وتوليد كود التفعيل بواسطة المالك',
        updated_at = now()
    WHERE id = p_teacher_id;

    -- إبطال أي أكواد قديمة غير مستخدمة
    UPDATE public.activation_codes
    SET is_used = true, used_at = now()
    WHERE teacher_id = p_teacher_id AND is_used = false;

    -- إدراج الكود الجديد
    INSERT INTO public.activation_codes (teacher_id, code_hash, plain_code, is_used, created_by)
    VALUES (p_teacher_id, crypt(v_code, gen_salt('bf')), v_code, false, auth.uid());

    -- توثيق العملية
    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'APPROVE',
        jsonb_build_object('code', v_code, 'school', v_teacher.school_name, 'phone', v_teacher.phone, 'timestamp', now())
    );

    -- إرسال إشعار داخلي للمعلم
    INSERT INTO public.teacher_notifications (teacher_id, notification_type, title, message)
    VALUES (
        p_teacher_id,
        'ACTIVATION',
        'تمت الموافقة على طلبك!',
        'مرحباً بك في تطبيق رقيب. تمت الموافقة على انضمامك، كود التفعيل الخاص بك هو: ' || v_code
    );

    RETURN jsonb_build_object('success', true, 'code', v_code, 'message', 'تمت الموافقة وتوليد كود التفعيل بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_approve_teacher(UUID, TEXT, TEXT) TO anon, authenticated, service_role;

-- رفض طلب المعلم
CREATE OR REPLACE FUNCTION public.rpc_reject_teacher(
    p_teacher_id UUID,
    p_reason TEXT DEFAULT 'طلب غير مستوفٍ للشروط',
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

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

    UPDATE public.activation_codes
    SET is_used = true, used_at = now()
    WHERE teacher_id = p_teacher_id AND is_used = false;

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
        'حالة طلب الانضمام',
        'نعتذر، تم رفض الطلب للأسباب التالية: ' || p_reason
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم رفض الطلب بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_reject_teacher(UUID, TEXT, TEXT) TO anon, authenticated, service_role;

-- تعليق حساب المعلم
CREATE OR REPLACE FUNCTION public.rpc_suspend_teacher(
    p_teacher_id UUID,
    p_reason TEXT DEFAULT 'إيقاف إداري مؤقت',
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

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

    RETURN jsonb_build_object('success', true, 'message', 'تم تعليق الحساب بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_suspend_teacher(UUID, TEXT, TEXT) TO anon, authenticated, service_role;

-- إعادة تفعيل الحساب المعلق
CREATE OR REPLACE FUNCTION public.rpc_reactivate_teacher(
    p_teacher_id UUID,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

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
GRANT EXECUTE ON FUNCTION public.rpc_reactivate_teacher(UUID, TEXT) TO anon, authenticated, service_role;

-- ==============================================================================
-- 8. دوال المعلمين (الاستعلام الآمن + التفعيل بكود أحادي الاستخدام)
-- ==============================================================================

-- استعلام آمن عن المعلم عند تسجيل الدخول
CREATE OR REPLACE FUNCTION public.rpc_get_teacher_by_login(
    p_identifier TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_clean TEXT;
    v_teacher RECORD;
BEGIN
    v_clean := trim(p_identifier);
    IF length(v_clean) = 0 THEN
        RETURN NULL;
    END IF;

    SELECT id, full_name, phone, email, password_hash, school_name, governorate, district, subject, education_stage, grade, section, curriculum_edition, academic_year, request_status, is_activated, review_notes, device_model
    INTO v_teacher
    FROM public.teachers
    WHERE email = v_clean OR phone = v_clean
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    RETURN row_to_json(v_teacher);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_get_teacher_by_login(TEXT) TO anon, authenticated, service_role;

-- تفعيل الحساب بكود الأمان (أحادي الاستخدام + يتطلب موافقة المالك، ويدعم الأكواد المخصصة وأكواد الدفعات)
CREATE OR REPLACE FUNCTION public.rpc_activate_with_code(
    p_identifier TEXT,
    p_code TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_teacher RECORD;
    v_clean_id TEXT;
    v_clean_code TEXT;
    v_code_rec RECORD;
    v_code_id UUID;
BEGIN
    v_clean_id := trim(p_identifier);
    v_clean_code := upper(trim(p_code));

    IF length(v_clean_id) = 0 OR length(v_clean_code) = 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'يرجى إدخال رقم الهاتف/البريد وكود التفعيل');
    END IF;

    SELECT * INTO v_teacher
    FROM public.teachers
    WHERE email = v_clean_id OR phone = v_clean_id
    LIMIT 1;

    -- إذا كان المعلم غير موجود بعد في قاعدة البيانات، نتحقق من وجود كود صالح
    IF NOT FOUND THEN
        SELECT * INTO v_code_rec FROM public.activation_codes
        WHERE (assigned_phone = v_clean_id OR (assigned_phone IS NULL OR assigned_phone = '') OR teacher_id IS NULL)
          AND upper(plain_code) = v_clean_code
        ORDER BY created_at DESC
        LIMIT 1;

        IF NOT FOUND THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل غير صحيح أو غير مسجل في النظام');
        END IF;

        IF COALESCE(v_code_rec.is_revoked, false) THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل هذا تم إلغاؤه من قِبل إدارة النظام');
        ELSIF v_code_rec.is_used THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل هذا تم استخدامه مسبقاً ولا يمكن استخدامه مرة أخرى');
        ELSIF v_code_rec.expires_at <= now() THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل منتهي الصلاحية');
        END IF;

        -- إنشاء حساب المعلم تلقائياً وتفعيله فوراً لمدة عام
        INSERT INTO public.teachers (
            full_name,
            phone,
            email,
            password_hash,
            school_name,
            governorate,
            subject,
            stage,
            grade,
            section,
            request_status,
            is_activated,
            review_notes
        )
        VALUES (
            COALESCE(NULLIF(v_code_rec.assigned_name, ''), 'أستاذ معلم'),
            v_clean_id,
            v_clean_id || '@rqeeb.edu',
            '123456',
            'مدرسة يمنية',
            'أمانة العاصمة',
            'الرياضيات',
            'مرحلة التعليم الأساسي',
            '9/ إعدادي',
            'أ',
            'APPROVED',
            true,
            'تم التفعيل المباشر بكود الأمان المعتمد'
        )
        RETURNING * INTO v_teacher;

        UPDATE public.activation_codes
        SET is_used = true,
            used_at = now(),
            teacher_id = v_teacher.id,
            assigned_phone = v_clean_id
        WHERE id = v_code_rec.id;

        INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
        VALUES (
            v_teacher.auth_user_id,
            v_teacher.id,
            v_teacher.full_name,
            'TEACHER_REGISTER_AND_ACTIVATED',
            jsonb_build_object('code', v_clean_code, 'phone', v_clean_id, 'duration_days', v_code_rec.duration_days)
        );

        RETURN jsonb_build_object(
            'success', true,
            'message', 'تم تفعيل حسابك بنجاح بكود الأمان المعتمد! مرحباً بك في رقيب',
            'teacher', row_to_json(v_teacher)
        );
    END IF;

    -- إذا كان الحساب موقوفاً أو مرفوضاً
    IF v_teacher.request_status = 'REJECTED' THEN
        RETURN jsonb_build_object('success', false, 'error', 'طلب التسجيل مرفوض من قِبل إدارة النظام');
    ELSIF v_teacher.request_status = 'SUSPENDED' THEN
        RETURN jsonb_build_object('success', false, 'error', 'الحساب موقوف إدارياً، يرجى مراجعة إدارة رقيب');
    END IF;

    -- إذا كان مفعل مسبقاً
    IF v_teacher.is_activated AND v_teacher.request_status = 'APPROVED' THEN
        RETURN jsonb_build_object(
            'success', true,
            'message', 'حسابك مفعل مسبقاً ومعتمد في النظام',
            'teacher', row_to_json(v_teacher)
        );
    END IF;

    -- استهلاك الكود ذرياً (Atomic Single-Use Update)
    UPDATE public.activation_codes
    SET is_used = true,
        used_at = now(),
        teacher_id = v_teacher.id,
        assigned_phone = v_teacher.phone
    WHERE id = (
        SELECT id FROM public.activation_codes
        WHERE (teacher_id = v_teacher.id OR assigned_phone = v_teacher.phone OR (teacher_id IS NULL AND (assigned_phone IS NULL OR assigned_phone = '')))
          AND upper(plain_code) = v_clean_code
          AND is_used = false
          AND COALESCE(is_revoked, false) = false
          AND expires_at > now()
        ORDER BY CASE WHEN teacher_id = v_teacher.id THEN 0 ELSE 1 END, created_at DESC
        LIMIT 1
    )
    RETURNING id INTO v_code_id;

    IF v_code_id IS NULL THEN
        IF EXISTS (
            SELECT 1 FROM public.activation_codes
            WHERE upper(plain_code) = v_clean_code
              AND is_revoked = true
        ) THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل هذا تم إلغاؤه من قِبل إدارة النظام');
        ELSIF EXISTS (
            SELECT 1 FROM public.activation_codes
            WHERE upper(plain_code) = v_clean_code
              AND is_used = true
        ) THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل هذا تم استخدامه مسبقاً ولا يمكن استخدامه مرة أخرى');
        ELSIF EXISTS (
            SELECT 1 FROM public.activation_codes
            WHERE upper(plain_code) = v_clean_code
              AND expires_at <= now()
        ) THEN
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل منتهي الصلاحية');
        ELSE
            RETURN jsonb_build_object('success', false, 'error', 'كود التفعيل غير صحيح أو غير متطابق مع رقم الهاتف');
        END IF;
    END IF;

    -- اعتماد وتفعيل المعلم
    UPDATE public.teachers
    SET request_status = 'APPROVED',
        is_activated = true,
        review_notes = 'تم التفعيل والاعتماد بواسطة كود الأمان السنوي',
        updated_at = now()
    WHERE id = v_teacher.id;

    -- توثيق العملية في سجل الرقابة
    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        v_teacher.auth_user_id,
        v_teacher.id,
        v_teacher.full_name,
        'TEACHER_ACTIVATED',
        jsonb_build_object('code', v_clean_code, 'phone', v_teacher.phone, 'school', v_teacher.school_name)
    );

    RETURN jsonb_build_object(
        'success', true,
        'message', 'تم تفعيل حسابك بنجاح لمدة عام كامل! يمكنك الآن استخدام تطبيق رقيب',
        'teacher', row_to_json(v_teacher)
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_activate_with_code(TEXT, TEXT) TO anon, authenticated, service_role;

-- ==============================================================================
-- 9. إدراج سجل المالك الأساسي في owner_config
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
ON CONFLICT (owner_email) DO NOTHING;

-- ==============================================================================
-- 10. جداول مكتبة المحتوى، إعلانات التطبيق، والمحتوى الديناميكي (التطوير الشامل للمالك)
-- ==============================================================================

-- جدول مكتبة المحتوى التعليمي والملفات (PDF، صور، أوراق عمل، اختبارات)
CREATE TABLE IF NOT EXISTS public.educational_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    resource_type TEXT NOT NULL DEFAULT 'PDF' CHECK (resource_type IN ('PDF', 'IMAGE', 'WORKSHEET', 'EXAM', 'OTHER')),
    subject TEXT NOT NULL DEFAULT 'الرياضيات',
    grade TEXT NOT NULL DEFAULT 'عام',
    unit_name TEXT NOT NULL DEFAULT '',
    lesson_name TEXT NOT NULL DEFAULT '',
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول إعلانات وتنبيهات التطبيق الديناميكية
CREATE TABLE IF NOT EXISTS public.app_announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'BANNER' CHECK (type IN ('BANNER', 'POPUP', 'NOTICE', 'UPDATE')),
    target_grade TEXT NOT NULL DEFAULT 'ALL',
    target_subject TEXT NOT NULL DEFAULT 'ALL',
    is_active BOOLEAN NOT NULL DEFAULT true,
    priority INT NOT NULL DEFAULT 0,
    link_url TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- جدول المحتوى الديناميكي والنصوص ومعلومات الدعم الفني
CREATE TABLE IF NOT EXISTS public.app_dynamic_content (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_key TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL DEFAULT 'GENERAL' CHECK (category IN ('GENERAL', 'SUPPORT', 'CURRICULUM', 'INSTRUCTIONS')),
    title TEXT NOT NULL,
    content_value TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- فهارس الجداول الجديدة
CREATE INDEX IF NOT EXISTS idx_resources_grade_subject ON public.educational_resources(grade, subject, is_public);
CREATE INDEX IF NOT EXISTS idx_resources_type ON public.educational_resources(resource_type);
CREATE INDEX IF NOT EXISTS idx_announcements_active ON public.app_announcements(is_active, priority DESC);
CREATE INDEX IF NOT EXISTS idx_dynamic_content_key ON public.app_dynamic_content(content_key);

-- تفعيل RLS على الجداول الجديدة
ALTER TABLE public.educational_resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_dynamic_content ENABLE ROW LEVEL SECURITY;

-- سياسات مكتبة المحتوى (العامة والمعلمون يقرؤون العام، المالك يتحكم بالكامل)
DROP POLICY IF EXISTS "Public read educational resources" ON public.educational_resources;
CREATE POLICY "Public read educational resources"
    ON public.educational_resources FOR SELECT
    USING (is_public = true OR public.is_owner() OR auth.role() = 'service_role');

DROP POLICY IF EXISTS "Owner modify educational resources" ON public.educational_resources;
CREATE POLICY "Owner modify educational resources"
    ON public.educational_resources FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- سياسات الإعلانات
DROP POLICY IF EXISTS "Public read announcements" ON public.app_announcements;
CREATE POLICY "Public read announcements"
    ON public.app_announcements FOR SELECT
    USING (is_active = true OR public.is_owner() OR auth.role() = 'service_role');

DROP POLICY IF EXISTS "Owner modify announcements" ON public.app_announcements;
CREATE POLICY "Owner modify announcements"
    ON public.app_announcements FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- سياسات المحتوى الديناميكي
DROP POLICY IF EXISTS "Public read dynamic content" ON public.app_dynamic_content;
CREATE POLICY "Public read dynamic content"
    ON public.app_dynamic_content FOR SELECT
    USING (is_active = true OR public.is_owner() OR auth.role() = 'service_role');

DROP POLICY IF EXISTS "Owner modify dynamic content" ON public.app_dynamic_content;
CREATE POLICY "Owner modify dynamic content"
    ON public.app_dynamic_content FOR ALL
    USING (public.is_owner() OR auth.role() = 'service_role');

-- ==============================================================================
-- 11. الدوال الإدارية المتقدمة للوحة المالك الشاملة
-- ==============================================================================

-- 1) إضافة معلم يدويًا بواسطة المالك مع إنشاء كود فوري
CREATE OR REPLACE FUNCTION public.rpc_add_teacher_manual(
    p_full_name TEXT,
    p_phone TEXT,
    p_school_name TEXT,
    p_governorate TEXT DEFAULT 'أمانة العاصمة',
    p_district TEXT DEFAULT '',
    p_subject TEXT DEFAULT 'الرياضيات',
    p_stage TEXT DEFAULT 'مرحلة التعليم الأساسي',
    p_grade TEXT DEFAULT '9/ إعدادي',
    p_section TEXT DEFAULT 'أ',
    p_curriculum_edition TEXT DEFAULT 'طبعة صنعاء (وزارة التربية والتعليم - صنعاء)',
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
    v_teacher_id UUID;
    v_code TEXT;
    v_clean_phone TEXT;
    v_clean_name TEXT;
    v_auto_email TEXT;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: هذه العملية محصورة بمالك النظام المعتمد فقط');
    END IF;

    v_clean_name := trim(p_full_name);
    v_clean_phone := trim(p_phone);

    IF length(v_clean_name) = 0 OR length(v_clean_phone) = 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'يرجى إدخال اسم المعلم ورقم هاتفه');
    END IF;

    IF EXISTS (SELECT 1 FROM public.teachers WHERE phone = v_clean_phone) THEN
        RETURN jsonb_build_object('success', false, 'error', 'رقم الهاتف مسجل مسبقاً لمعلم آخر');
    END IF;

    v_auto_email := v_clean_phone || '@rqeeb.edu';

    v_code := 'RQ-' || (1000 + floor(random() * 9000))::TEXT || '-' || chr(65 + floor(random() * 26)::INT);

    INSERT INTO public.teachers (
        full_name, phone, email, school_name, governorate, district, subject,
        education_stage, grade, section, curriculum_edition,
        request_status, is_activated, review_notes
    ) VALUES (
        v_clean_name, v_clean_phone, v_auto_email, p_school_name, p_governorate, p_district, p_subject,
        p_stage, p_grade, p_section, p_curriculum_edition,
        'APPROVED', false, 'تمت الإضافة يدويًا بواسطة المالك'
    ) RETURNING id INTO v_teacher_id;

    INSERT INTO public.activation_codes (teacher_id, code_hash, plain_code, is_used, created_by)
    VALUES (v_teacher_id, crypt(v_code, gen_salt('bf')), v_code, false, auth.uid());

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        v_teacher_id,
        v_clean_name,
        'MANUAL_ADD',
        jsonb_build_object(
            'phone', v_clean_phone,
            'school', p_school_name,
            'code', v_code,
            'subject', p_subject,
            'grade', p_grade
        )
    );

    RETURN jsonb_build_object(
        'success', true,
        'message', 'تمت إضافة المعلم وتوليد كود التفعيل بنجاح',
        'teacher_id', v_teacher_id,
        'code', v_code
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_add_teacher_manual(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon, authenticated, service_role;

-- 2) تعديل بيانات المعلم من لوحة المالك
CREATE OR REPLACE FUNCTION public.rpc_update_teacher_manual(
    p_teacher_id UUID,
    p_full_name TEXT,
    p_phone TEXT,
    p_school_name TEXT,
    p_governorate TEXT,
    p_district TEXT,
    p_subject TEXT,
    p_stage TEXT,
    p_grade TEXT,
    p_section TEXT,
    p_curriculum_edition TEXT,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    UPDATE public.teachers
    SET full_name = trim(p_full_name),
        phone = trim(p_phone),
        school_name = trim(p_school_name),
        governorate = trim(p_governorate),
        district = trim(p_district),
        subject = trim(p_subject),
        education_stage = trim(p_stage),
        grade = trim(p_grade),
        section = trim(p_section),
        curriculum_edition = trim(p_curriculum_edition),
        updated_at = now()
    WHERE id = p_teacher_id;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        trim(p_full_name),
        'UPDATE_TEACHER',
        jsonb_build_object('phone', trim(p_phone), 'school', trim(p_school_name))
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم تحديث بيانات المعلم بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_update_teacher_manual(UUID, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon, authenticated, service_role;

-- 3) حذف حساب المعلم نهائياً
CREATE OR REPLACE FUNCTION public.rpc_delete_teacher_manual(
    p_teacher_id UUID,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
    v_teacher RECORD;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    DELETE FROM public.teachers WHERE id = p_teacher_id;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'DELETE_TEACHER',
        jsonb_build_object('phone', v_teacher.phone, 'school', v_teacher.school_name)
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم حذف حساب المعلم بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_delete_teacher_manual(UUID, TEXT) TO anon, authenticated, service_role;

-- 4) توليد دفعة أكواد تفعيل مسبقة (100 كود أو عدد مخصص) لمدة عام كامل
CREATE OR REPLACE FUNCTION public.rpc_generate_batch_codes(
    p_count INT DEFAULT 100,
    p_prefix TEXT DEFAULT 'RQ',
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_generated INT := 0;
    v_code TEXT;
    v_clean_prefix TEXT;
    v_limit INT;
    v_expires_at TIMESTAMPTZ;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    v_limit := LEAST(GREATEST(COALESCE(p_count, 100), 1), 500);
    v_clean_prefix := COALESCE(NULLIF(trim(p_prefix), ''), 'RQ');
    v_expires_at := now() + INTERVAL '365 days'; -- مدة سنة كاملة

    FOR i IN 1..v_limit LOOP
        v_code := v_clean_prefix || '-' || (1000 + floor(random() * 9000))::TEXT || '-' || chr(65 + floor(random() * 26)::INT);
        BEGIN
            INSERT INTO public.activation_codes (
                teacher_id,
                code_hash,
                plain_code,
                assigned_name,
                assigned_phone,
                duration_days,
                expires_at,
                is_used,
                created_by
            )
            VALUES (
                NULL,
                crypt(v_code, gen_salt('bf')),
                v_code,
                '',
                '',
                365,
                v_expires_at,
                false,
                auth.uid()
            );
            v_generated := v_generated + 1;
        EXCEPTION WHEN unique_violation THEN
            -- تجنب التكرار
        END;
    END LOOP;

    INSERT INTO public.audit_logs (owner_user_id, action, details)
    VALUES (
        auth.uid(),
        'BATCH_CODES_GENERATED',
        jsonb_build_object('count', v_generated, 'prefix', v_clean_prefix)
    );

    RETURN jsonb_build_object(
        'success', true,
        'generated_count', v_generated,
        'message', 'تم إنشاء ' || v_generated || ' كود تفعيل بنجاح لمدة عام كامل'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_generate_batch_codes(INT, TEXT, TEXT) TO anon, authenticated, service_role;

-- 4.1) إنشاء كود تفعيل مخصص لمعلم محدد بمدة محددة
CREATE OR REPLACE FUNCTION public.rpc_create_activation_code(
    p_teacher_name TEXT DEFAULT '',
    p_teacher_phone TEXT DEFAULT '',
    p_duration_days INT DEFAULT 365,
    p_custom_code TEXT DEFAULT NULL,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_code TEXT;
    v_days INT;
    v_teacher_id UUID := NULL;
    v_new_id UUID;
    v_expires_at TIMESTAMPTZ;
    v_clean_name TEXT;
    v_clean_phone TEXT;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بمالك النظام');
    END IF;

    v_clean_name := trim(COALESCE(p_teacher_name, ''));
    v_clean_phone := trim(COALESCE(p_teacher_phone, ''));
    v_days := GREATEST(COALESCE(p_duration_days, 365), 1);
    v_expires_at := now() + (v_days || ' days')::INTERVAL;

    IF length(v_clean_phone) > 0 THEN
        SELECT id INTO v_teacher_id FROM public.teachers WHERE phone = v_clean_phone LIMIT 1;
    END IF;

    IF p_custom_code IS NOT NULL AND length(trim(p_custom_code)) > 0 THEN
        v_code := upper(trim(p_custom_code));
    ELSE
        v_code := 'RQ-' || (1000 + floor(random() * 9000))::TEXT || '-' || chr(65 + floor(random() * 26)::INT);
    END IF;

    INSERT INTO public.activation_codes (
        teacher_id,
        code_hash,
        plain_code,
        assigned_name,
        assigned_phone,
        duration_days,
        expires_at,
        is_used,
        created_by
    )
    VALUES (
        v_teacher_id,
        crypt(v_code, gen_salt('bf')),
        v_code,
        v_clean_name,
        v_clean_phone,
        v_days,
        v_expires_at,
        false,
        auth.uid()
    )
    RETURNING id INTO v_new_id;

    IF v_teacher_id IS NOT NULL THEN
        UPDATE public.teachers
        SET request_status = 'APPROVED',
            review_notes = 'تم تعيين كود التفعيل بواسطة المالك',
            updated_at = now()
        WHERE id = v_teacher_id;
    END IF;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        v_teacher_id,
        COALESCE(NULLIF(v_clean_name, ''), 'كود مستقل'),
        'CREATE_CODE',
        jsonb_build_object('code', v_code, 'phone', v_clean_phone, 'duration_days', v_days)
    );

    RETURN jsonb_build_object(
        'success', true,
        'id', v_new_id,
        'code', v_code,
        'teacher_name', v_clean_name,
        'teacher_phone', v_clean_phone,
        'duration_days', v_days,
        'expires_at', v_expires_at,
        'status', 'AVAILABLE',
        'message', 'تم إنشاء كود التفعيل بنجاح'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_create_activation_code(TEXT, TEXT, INT, TEXT, TEXT) TO anon, authenticated, service_role;

-- 5) استعراض جميع أكواد التفعيل للمالك مع حالتها وتفاصيل المعلم والمدة
CREATE OR REPLACE FUNCTION public.rpc_get_all_activation_codes(
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_array();
    END IF;

    RETURN (
        SELECT COALESCE(jsonb_agg(
            jsonb_build_object(
                'id', c.id,
                'code', c.plain_code,
                'teacher_id', c.teacher_id,
                'teacher_name', CASE
                    WHEN length(COALESCE(c.assigned_name, '')) > 0 THEN c.assigned_name
                    WHEN t.full_name IS NOT NULL THEN t.full_name
                    ELSE 'كود عام متاح'
                END,
                'teacher_phone', CASE
                    WHEN length(COALESCE(c.assigned_phone, '')) > 0 THEN c.assigned_phone
                    WHEN t.phone IS NOT NULL THEN t.phone
                    ELSE ''
                END,
                'duration_days', COALESCE(c.duration_days, 365),
                'is_used', c.is_used,
                'is_revoked', COALESCE(c.is_revoked, false),
                'revoked_reason', COALESCE(c.revoked_reason, ''),
                'created_at', c.created_at,
                'used_at', c.used_at,
                'expires_at', c.expires_at,
                'status', CASE
                    WHEN COALESCE(c.is_revoked, false) THEN 'REVOKED'
                    WHEN c.is_used THEN 'USED'
                    WHEN c.expires_at < now() THEN 'EXPIRED'
                    ELSE 'AVAILABLE'
                END
            ) ORDER BY c.created_at DESC
        ), '[]'::jsonb)
        FROM public.activation_codes c
        LEFT JOIN public.teachers t ON c.teacher_id = t.id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_get_all_activation_codes(TEXT) TO anon, authenticated, service_role;

-- 6) إلغاء كود تفعيل
CREATE OR REPLACE FUNCTION public.rpc_revoke_activation_code(
    p_code_id UUID,
    p_reason TEXT DEFAULT 'إلغاء بواسطة الإدارة',
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_rec RECORD;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    SELECT * INTO v_rec FROM public.activation_codes WHERE id = p_code_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'الكود غير موجود');
    END IF;

    UPDATE public.activation_codes
    SET is_revoked = true,
        revoked_reason = COALESCE(NULLIF(trim(p_reason), ''), 'إلغاء بواسطة الإدارة'),
        revoked_at = now()
    WHERE id = p_code_id;

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, action, details)
    VALUES (
        auth.uid(),
        v_rec.teacher_id,
        'CODE_REVOKED',
        jsonb_build_object('code', v_rec.plain_code, 'reason', p_reason)
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم إلغاء كود التفعيل بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_revoke_activation_code(UUID, TEXT, TEXT) TO anon, authenticated, service_role;

-- 7) إصدار كود بديل لمعلم محدد
CREATE OR REPLACE FUNCTION public.rpc_reissue_activation_code(
    p_teacher_id UUID,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
    v_teacher RECORD;
    v_code TEXT;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    SELECT * INTO v_teacher FROM public.teachers WHERE id = p_teacher_id;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'المعلم غير موجود');
    END IF;

    UPDATE public.activation_codes
    SET is_revoked = true,
        revoked_reason = 'تم إصدار كود بديل جديد بواسطة المالك',
        revoked_at = now()
    WHERE teacher_id = p_teacher_id AND is_used = false AND is_revoked = false;

    v_code := 'RQ-' || (1000 + floor(random() * 9000))::TEXT || '-' || chr(65 + floor(random() * 26)::INT);

    INSERT INTO public.activation_codes (teacher_id, code_hash, plain_code, is_used, created_by)
    VALUES (p_teacher_id, crypt(v_code, gen_salt('bf')), v_code, false, auth.uid());

    INSERT INTO public.audit_logs (owner_user_id, teacher_id, teacher_name, action, details)
    VALUES (
        auth.uid(),
        p_teacher_id,
        v_teacher.full_name,
        'CODE_REISSUED',
        jsonb_build_object('new_code', v_code, 'phone', v_teacher.phone)
    );

    RETURN jsonb_build_object('success', true, 'code', v_code, 'message', 'تم إصدار كود بديل بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_reissue_activation_code(UUID, TEXT) TO anon, authenticated, service_role;

-- 8) إدارة مكتبة المحتوى (إضافة وتعديل مادة تعليمية)
CREATE OR REPLACE FUNCTION public.rpc_add_or_update_resource(
    p_id UUID DEFAULT NULL,
    p_title TEXT DEFAULT '',
    p_description TEXT DEFAULT '',
    p_resource_type TEXT DEFAULT 'PDF',
    p_subject TEXT DEFAULT 'الرياضيات',
    p_grade TEXT DEFAULT 'عام',
    p_unit_name TEXT DEFAULT '',
    p_lesson_name TEXT DEFAULT '',
    p_file_url TEXT DEFAULT '',
    p_file_size_bytes BIGINT DEFAULT 0,
    p_is_public BOOLEAN DEFAULT true,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
    v_res_id UUID;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    IF p_id IS NOT NULL AND EXISTS (SELECT 1 FROM public.educational_resources WHERE id = p_id) THEN
        UPDATE public.educational_resources
        SET title = trim(p_title),
            description = trim(p_description),
            resource_type = p_resource_type,
            subject = p_subject,
            grade = p_grade,
            unit_name = p_unit_name,
            lesson_name = p_lesson_name,
            file_url = p_file_url,
            file_size_bytes = p_file_size_bytes,
            is_public = p_is_public,
            updated_at = now()
        WHERE id = p_id;
        v_res_id := p_id;
    ELSE
        INSERT INTO public.educational_resources (
            title, description, resource_type, subject, grade,
            unit_name, lesson_name, file_url, file_size_bytes, is_public, created_by
        ) VALUES (
            trim(p_title), trim(p_description), p_resource_type, p_subject, p_grade,
            p_unit_name, p_lesson_name, p_file_url, p_file_size_bytes, p_is_public, auth.uid()
        ) RETURNING id INTO v_res_id;
    END IF;

    INSERT INTO public.audit_logs (owner_user_id, action, details)
    VALUES (
        auth.uid(),
        'RESOURCE_SAVED',
        jsonb_build_object('id', v_res_id, 'title', p_title, 'type', p_resource_type)
    );

    RETURN jsonb_build_object('success', true, 'id', v_res_id, 'message', 'تم حفظ المادة التعليمية بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_add_or_update_resource(UUID, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, BIGINT, BOOLEAN, TEXT) TO anon, authenticated, service_role;

-- 9) حذف مادة تعليمية
CREATE OR REPLACE FUNCTION public.rpc_delete_resource(
    p_resource_id UUID,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    DELETE FROM public.educational_resources WHERE id = p_resource_id;

    RETURN jsonb_build_object('success', true, 'message', 'تم حذف المادة بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_delete_resource(UUID, TEXT) TO anon, authenticated, service_role;

-- 10) استعراض المواد التعليمية
CREATE OR REPLACE FUNCTION public.rpc_get_educational_resources(
    p_grade TEXT DEFAULT NULL,
    p_subject TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
BEGIN
    RETURN (
        SELECT COALESCE(jsonb_agg(row_to_json(r) ORDER BY r.created_at DESC), '[]'::jsonb)
        FROM public.educational_resources r
        WHERE (p_grade IS NULL OR r.grade = p_grade OR r.grade = 'عام')
          AND (p_subject IS NULL OR r.subject = p_subject OR r.subject = 'عام')
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_get_educational_resources(TEXT, TEXT) TO anon, authenticated, service_role;

-- 11) إدارة الإعلانات والتنبيهات
CREATE OR REPLACE FUNCTION public.rpc_add_or_update_announcement(
    p_id UUID DEFAULT NULL,
    p_title TEXT DEFAULT '',
    p_content TEXT DEFAULT '',
    p_type TEXT DEFAULT 'BANNER',
    p_target_grade TEXT DEFAULT 'ALL',
    p_target_subject TEXT DEFAULT 'ALL',
    p_is_active BOOLEAN DEFAULT true,
    p_priority INT DEFAULT 0,
    p_link_url TEXT DEFAULT '',
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
    v_ann_id UUID;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    IF p_id IS NOT NULL AND EXISTS (SELECT 1 FROM public.app_announcements WHERE id = p_id) THEN
        UPDATE public.app_announcements
        SET title = trim(p_title),
            content = trim(p_content),
            type = p_type,
            target_grade = p_target_grade,
            target_subject = p_target_subject,
            is_active = p_is_active,
            priority = p_priority,
            link_url = p_link_url,
            updated_at = now()
        WHERE id = p_id;
        v_ann_id := p_id;
    ELSE
        INSERT INTO public.app_announcements (
            title, content, type, target_grade, target_subject, is_active, priority, link_url
        ) VALUES (
            trim(p_title), trim(p_content), p_type, p_target_grade, p_target_subject, p_is_active, p_priority, p_link_url
        ) RETURNING id INTO v_ann_id;
    END IF;

    INSERT INTO public.audit_logs (owner_user_id, action, details)
    VALUES (
        auth.uid(),
        'ANNOUNCEMENT_SAVED',
        jsonb_build_object('id', v_ann_id, 'title', p_title, 'is_active', p_is_active)
    );

    RETURN jsonb_build_object('success', true, 'id', v_ann_id, 'message', 'تم حفظ الإعلان بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_add_or_update_announcement(UUID, TEXT, TEXT, TEXT, TEXT, TEXT, BOOLEAN, INT, TEXT, TEXT) TO anon, authenticated, service_role;

CREATE OR REPLACE FUNCTION public.rpc_delete_announcement(
    p_announcement_id UUID,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    DELETE FROM public.app_announcements WHERE id = p_announcement_id;

    RETURN jsonb_build_object('success', true, 'message', 'تم حذف الإعلان بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_delete_announcement(UUID, TEXT) TO anon, authenticated, service_role;

CREATE OR REPLACE FUNCTION public.rpc_get_app_announcements(
    p_grade TEXT DEFAULT 'ALL',
    p_subject TEXT DEFAULT 'ALL'
)
RETURNS JSONB AS $$
BEGIN
    RETURN (
        SELECT COALESCE(jsonb_agg(row_to_json(a) ORDER BY a.priority DESC, a.created_at DESC), '[]'::jsonb)
        FROM public.app_announcements a
        WHERE a.is_active = true
          AND (a.target_grade = 'ALL' OR a.target_grade = p_grade)
          AND (a.target_subject = 'ALL' OR a.target_subject = p_subject)
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_get_app_announcements(TEXT, TEXT) TO anon, authenticated, service_role;

-- 12) إدارة المحتوى الديناميكي والنصوص ومعلومات التواصل
CREATE OR REPLACE FUNCTION public.rpc_add_or_update_dynamic_content(
    p_key TEXT,
    p_category TEXT DEFAULT 'GENERAL',
    p_title TEXT DEFAULT '',
    p_value TEXT DEFAULT '',
    p_is_active BOOLEAN DEFAULT true,
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_is_auth BOOLEAN := false;
BEGIN
    IF public.is_owner() THEN
        v_is_auth := true;
    ELSIF p_owner_pin IS NOT NULL AND length(trim(p_owner_pin)) > 0 THEN
        SELECT (owner_pin_hash = crypt(trim(p_owner_pin), owner_pin_hash)) INTO v_is_auth
        FROM public.owner_config
        WHERE owner_email = 'abdulrqeebfarag2018@gmail.com'
        LIMIT 1;
    END IF;

    IF NOT COALESCE(v_is_auth, false) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    INSERT INTO public.app_dynamic_content (content_key, category, title, content_value, is_active, updated_at)
    VALUES (trim(p_key), p_category, trim(p_title), p_value, p_is_active, now())
    ON CONFLICT (content_key) DO UPDATE
    SET category = EXCLUDED.category,
        title = EXCLUDED.title,
        content_value = EXCLUDED.content_value,
        is_active = EXCLUDED.is_active,
        updated_at = now();

    INSERT INTO public.audit_logs (owner_user_id, action, details)
    VALUES (
        auth.uid(),
        'DYNAMIC_CONTENT_UPDATED',
        jsonb_build_object('key', p_key, 'title', p_title)
    );

    RETURN jsonb_build_object('success', true, 'message', 'تم تحديث المحتوى بنجاح');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_add_or_update_dynamic_content(TEXT, TEXT, TEXT, TEXT, BOOLEAN, TEXT) TO anon, authenticated, service_role;

CREATE OR REPLACE FUNCTION public.rpc_get_dynamic_content(
    p_category TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
BEGIN
    RETURN (
        SELECT COALESCE(jsonb_agg(row_to_json(d) ORDER BY d.content_key ASC), '[]'::jsonb)
        FROM public.app_dynamic_content d
        WHERE d.is_active = true
          AND (p_category IS NULL OR d.category = p_category)
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_get_dynamic_content(TEXT) TO anon, authenticated, service_role;

-- إدراج محتويات أولية مفيدة لتطبيق رقيب
INSERT INTO public.app_dynamic_content (content_key, category, title, content_value, is_active)
VALUES
    ('owner_whatsapp', 'SUPPORT', 'رقم واتساب الإدارة والدعم الفني', '+967785502919', true),
    ('owner_phone', 'SUPPORT', 'هاتف الاتصال المباشر بالمالك', '+967785502919', true),
    ('app_guide_text', 'INSTRUCTIONS', 'إرشادات المعلم اليمني', 'مرحباً بك في رقيب مساعد المعلم الذكي. يمكنك تحضير الدروس وفق الخطة القياسية وطباعة كشوفات الدرجات والاختبارات المعتمدة.', true),
    ('curriculum_info', 'CURRICULUM', 'بيان المناهج المعتمدة', 'يدعم التطبيق المناهج المدرسية الصادرة عن وزارة التربية والتعليم للجمهورية اليمنية (طبعتي صنعاء وعدن) للمرحلتين الأساسية والثانوية.', true)
ON CONFLICT (content_key) DO NOTHING;

INSERT INTO public.app_announcements (title, content, type, target_grade, target_subject, priority, is_active)
VALUES (
    'تحديث نظام رقيب 2024-2025م',
    'أهلاً بكم في النسخة المتطورة من رقيب – مساعد المعلم الذكي. تم إدراج المناهج اليمنية المحدثة وخدمة المزامنة السحابية الفورية.',
    'BANNER',
    'ALL',
    'ALL',
    10,
    true
) ON CONFLICT DO NOTHING;

-- ==============================================================================
-- 12. الأكواد الـ 100 المعتمدة رسميًا لمالك التطبيق (T001 - T100) لمدة عام كامل
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.rpc_seed_official_100_codes(
    p_owner_pin TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_codes TEXT[] := ARRAY[
        'RQ-4847-T001','RQ-3766-T002','RQ-2685-T003','RQ-1604-T004','RQ-9523-T005',
        'RQ-8442-T006','RQ-7361-T007','RQ-6280-T008','RQ-5199-T009','RQ-4118-T010',
        'RQ-3037-T011','RQ-1956-T012','RQ-9875-T013','RQ-8794-T014','RQ-7713-T015',
        'RQ-6632-T016','RQ-5551-T017','RQ-4470-T018','RQ-3389-T019','RQ-2308-T020',
        'RQ-1227-T021','RQ-9146-T022','RQ-8065-T023','RQ-6984-T024','RQ-5903-T025',
        'RQ-4822-T026','RQ-3741-T027','RQ-2660-T028','RQ-1579-T029','RQ-9498-T030',
        'RQ-8417-T031','RQ-7336-T032','RQ-6255-T033','RQ-5174-T034','RQ-4093-T035',
        'RQ-3012-T036','RQ-1931-T037','RQ-9850-T038','RQ-8769-T039','RQ-7688-T040',
        'RQ-6607-T041','RQ-5526-T042','RQ-4445-T043','RQ-3364-T044','RQ-2283-T045',
        'RQ-1202-T046','RQ-9121-T047','RQ-8040-T048','RQ-6959-T049','RQ-5878-T050',
        'RQ-4797-T051','RQ-3716-T052','RQ-2635-T053','RQ-1554-T054','RQ-9473-T055',
        'RQ-8392-T056','RQ-7311-T057','RQ-6230-T058','RQ-5149-T059','RQ-4068-T060',
        'RQ-2987-T061','RQ-1906-T062','RQ-9825-T063','RQ-8744-T064','RQ-7663-T065',
        'RQ-6582-T066','RQ-5501-T067','RQ-4420-T068','RQ-3339-T069','RQ-2258-T070',
        'RQ-1177-T071','RQ-9096-T072','RQ-8015-T073','RQ-6934-T074','RQ-5853-T075',
        'RQ-4772-T076','RQ-3691-T077','RQ-2610-T078','RQ-1529-T079','RQ-9448-T080',
        'RQ-8367-T081','RQ-7286-T082','RQ-6205-T083','RQ-5124-T084','RQ-4043-T085',
        'RQ-2962-T086','RQ-1881-T087','RQ-9800-T088','RQ-8719-T089','RQ-7638-T090',
        'RQ-6557-T091','RQ-5476-T092','RQ-4395-T093','RQ-3314-T094','RQ-2233-T095',
        'RQ-1152-T096','RQ-9071-T097','RQ-7990-T098','RQ-6909-T099','RQ-5828-T100'
    ];
    v_c TEXT;
    v_count INT := 0;
BEGIN
    IF NOT public.check_owner_auth(p_owner_pin) THEN
        RETURN jsonb_build_object('success', false, 'error', 'غير مصرح: محصور بالمالك');
    END IF;

    FOREACH v_c IN ARRAY v_codes LOOP
        INSERT INTO public.activation_codes (
            teacher_id,
            code_hash,
            plain_code,
            assigned_name,
            assigned_phone,
            duration_days,
            expires_at,
            is_used,
            created_by
        )
        VALUES (
            NULL,
            crypt(v_c, gen_salt('bf')),
            v_c,
            'كود عام متاح',
            '',
            365,
            now() + INTERVAL '365 days',
            false,
            auth.uid()
        )
        ON CONFLICT (plain_code) DO NOTHING;
        v_count := v_count + 1;
    END LOOP;

    INSERT INTO public.audit_logs (owner_user_id, action, details)
    VALUES (
        auth.uid(),
        'SEED_OFFICIAL_100_CODES',
        jsonb_build_object('total', v_count, 'duration_days', 365)
    );

    RETURN jsonb_build_object(
        'success', true,
        'count', v_count,
        'message', 'تم تثبيت وتفعيل 100 كود معتمد بنجاح في قاعدة البيانات السحابية'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
GRANT EXECUTE ON FUNCTION public.rpc_seed_official_100_codes(TEXT) TO anon, authenticated, service_role;

-- إدراج مباشر للأكواد الـ 100 المعتمدة في جدول activation_codes عند تشغيل الملف
INSERT INTO public.activation_codes (plain_code, code_hash, duration_days, expires_at, is_used, assigned_name)
VALUES
    ('RQ-4847-T001', crypt('RQ-4847-T001', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3766-T002', crypt('RQ-3766-T002', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2685-T003', crypt('RQ-2685-T003', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1604-T004', crypt('RQ-1604-T004', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9523-T005', crypt('RQ-9523-T005', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8442-T006', crypt('RQ-8442-T006', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7361-T007', crypt('RQ-7361-T007', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6280-T008', crypt('RQ-6280-T008', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5199-T009', crypt('RQ-5199-T009', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4118-T010', crypt('RQ-4118-T010', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3037-T011', crypt('RQ-3037-T011', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1956-T012', crypt('RQ-1956-T012', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9875-T013', crypt('RQ-9875-T013', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8794-T014', crypt('RQ-8794-T014', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7713-T015', crypt('RQ-7713-T015', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6632-T016', crypt('RQ-6632-T016', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5551-T017', crypt('RQ-5551-T017', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4470-T018', crypt('RQ-4470-T018', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3389-T019', crypt('RQ-3389-T019', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2308-T020', crypt('RQ-2308-T020', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1227-T021', crypt('RQ-1227-T021', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9146-T022', crypt('RQ-9146-T022', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8065-T023', crypt('RQ-8065-T023', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6984-T024', crypt('RQ-6984-T024', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5903-T025', crypt('RQ-5903-T025', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4822-T026', crypt('RQ-4822-T026', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3741-T027', crypt('RQ-3741-T027', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2660-T028', crypt('RQ-2660-T028', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1579-T029', crypt('RQ-1579-T029', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9498-T030', crypt('RQ-9498-T030', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8417-T031', crypt('RQ-8417-T031', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7336-T032', crypt('RQ-7336-T032', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6255-T033', crypt('RQ-6255-T033', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5174-T034', crypt('RQ-5174-T034', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4093-T035', crypt('RQ-4093-T035', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3012-T036', crypt('RQ-3012-T036', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1931-T037', crypt('RQ-1931-T037', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9850-T038', crypt('RQ-9850-T038', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8769-T039', crypt('RQ-8769-T039', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7688-T040', crypt('RQ-7688-T040', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6607-T041', crypt('RQ-6607-T041', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5526-T042', crypt('RQ-5526-T042', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4445-T043', crypt('RQ-4445-T043', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3364-T044', crypt('RQ-3364-T044', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2283-T045', crypt('RQ-2283-T045', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1202-T046', crypt('RQ-1202-T046', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9121-T047', crypt('RQ-9121-T047', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8040-T048', crypt('RQ-8040-T048', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6959-T049', crypt('RQ-6959-T049', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5878-T050', crypt('RQ-5878-T050', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4797-T051', crypt('RQ-4797-T051', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3716-T052', crypt('RQ-3716-T052', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2635-T053', crypt('RQ-2635-T053', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1554-T054', crypt('RQ-1554-T054', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9473-T055', crypt('RQ-9473-T055', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8392-T056', crypt('RQ-8392-T056', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7311-T057', crypt('RQ-7311-T057', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6230-T058', crypt('RQ-6230-T058', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5149-T059', crypt('RQ-5149-T059', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4068-T060', crypt('RQ-4068-T060', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2987-T061', crypt('RQ-2987-T061', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1906-T062', crypt('RQ-1906-T062', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9825-T063', crypt('RQ-9825-T063', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8744-T064', crypt('RQ-8744-T064', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7663-T065', crypt('RQ-7663-T065', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6582-T066', crypt('RQ-6582-T066', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5501-T067', crypt('RQ-5501-T067', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4420-T068', crypt('RQ-4420-T068', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3339-T069', crypt('RQ-3339-T069', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2258-T070', crypt('RQ-2258-T070', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1177-T071', crypt('RQ-1177-T071', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9096-T072', crypt('RQ-9096-T072', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8015-T073', crypt('RQ-8015-T073', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6934-T074', crypt('RQ-6934-T074', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5853-T075', crypt('RQ-5853-T075', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4772-T076', crypt('RQ-4772-T076', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3691-T077', crypt('RQ-3691-T077', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2610-T078', crypt('RQ-2610-T078', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1529-T079', crypt('RQ-1529-T079', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9448-T080', crypt('RQ-9448-T080', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8367-T081', crypt('RQ-8367-T081', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7286-T082', crypt('RQ-7286-T082', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6205-T083', crypt('RQ-6205-T083', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5124-T084', crypt('RQ-5124-T084', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4043-T085', crypt('RQ-4043-T085', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2962-T086', crypt('RQ-2962-T086', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1881-T087', crypt('RQ-1881-T087', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9800-T088', crypt('RQ-9800-T088', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-8719-T089', crypt('RQ-8719-T089', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7638-T090', crypt('RQ-7638-T090', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6557-T091', crypt('RQ-6557-T091', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5476-T092', crypt('RQ-5476-T092', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-4395-T093', crypt('RQ-4395-T093', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-3314-T094', crypt('RQ-3314-T094', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-2233-T095', crypt('RQ-2233-T095', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-1152-T096', crypt('RQ-1152-T096', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-9071-T097', crypt('RQ-9071-T097', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-7990-T098', crypt('RQ-7990-T098', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-6909-T099', crypt('RQ-6909-T099', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح'),
    ('RQ-5828-T100', crypt('RQ-5828-T100', gen_salt('bf')), 365, now() + INTERVAL '365 days', false, 'كود عام متاح')
ON CONFLICT (plain_code) DO NOTHING;


