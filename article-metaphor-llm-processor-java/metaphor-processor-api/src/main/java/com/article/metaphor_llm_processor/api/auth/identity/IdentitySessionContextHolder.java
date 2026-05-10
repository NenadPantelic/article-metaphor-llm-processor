package com.article.metaphor_llm_processor.api.auth.identity;

public class IdentitySessionContextHolder {

    private static final ThreadLocal<IdentitySession> context = new ThreadLocal<>();

    public static void set(IdentitySession identitySession) {
        context.set(identitySession);
    }

    public static IdentitySession get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}
