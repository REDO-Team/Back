package com.redo.global.util;

import java.util.function.Supplier;

public final class CursorRequestValidator {

    public static final int MAX_PAGE_SIZE = 50;
    public static final int CONTRIBUTION_MAX_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;

    private CursorRequestValidator() {
    }

    public static void validate(
            Long cursor,
            int size,
            Supplier<? extends RuntimeException> exceptionSupplier
    ) {
        validate(cursor, size, MAX_PAGE_SIZE, exceptionSupplier);
    }

    public static void validate(
            Long cursor,
            int size,
            int maxPageSize,
            Supplier<? extends RuntimeException> exceptionSupplier
    ) {
        if ((cursor != null && cursor <= 0)
                || size < MIN_PAGE_SIZE
                || size > maxPageSize) {
            throw exceptionSupplier.get();
        }
    }
}
