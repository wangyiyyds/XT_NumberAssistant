package com.xianting.manage.common;

/** Holds the authenticated adminId for the current request thread. */
public final class AdminContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(Long adminId) {
        CURRENT.set(adminId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static Long require() {
        Long id = CURRENT.get();
        if (id == null) throw new ApiException(401, "unauthorized");
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
