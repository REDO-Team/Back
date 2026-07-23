package com.redo.global.ai.gemini.dto;

import java.util.Arrays;

public final class GeminiMedia {

    private final String mimeType;
    private final byte[] data;

    public GeminiMedia(String mimeType, byte[] data) {
        this.mimeType = mimeType;
        this.data = data == null ? null : Arrays.copyOf(data, data.length);
    }

    public String mimeType() {
        return mimeType;
    }

    public byte[] data() {
        return data == null ? null : Arrays.copyOf(data, data.length);
    }

    public int size() {
        return data == null ? 0 : data.length;
    }

    @Override
    public String toString() {
        return "GeminiMedia{mimeType='%s', size=%d}".formatted(mimeType, size());
    }
}
