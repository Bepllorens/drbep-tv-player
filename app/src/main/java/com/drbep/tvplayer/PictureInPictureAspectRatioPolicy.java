package com.drbep.tvplayer;

final class PictureInPictureAspectRatioPolicy {
    private static final float MIN_RATIO = 1f / 2.39f;
    private static final float MAX_RATIO = 2.39f;

    static final class Ratio {
        final int width;
        final int height;

        Ratio(int width, int height) {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }
    }

    private PictureInPictureAspectRatioPolicy() {
    }

    static Ratio resolve(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) {
            return new Ratio(16, 9);
        }
        float value = (float) videoWidth / (float) videoHeight;
        if (value < MIN_RATIO || value > MAX_RATIO) {
            return new Ratio(16, 9);
        }
        int divisor = greatestCommonDivisor(videoWidth, videoHeight);
        return new Ratio(videoWidth / divisor, videoHeight / divisor);
    }

    private static int greatestCommonDivisor(int left, int right) {
        int a = Math.abs(left);
        int b = Math.abs(right);
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return Math.max(1, a);
    }
}
