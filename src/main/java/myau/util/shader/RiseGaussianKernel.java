package myau.util.shader;

final class RiseGaussianKernel {
    private RiseGaussianKernel() {
    }

    static float[] compute(int radius) {
        radius = Math.max(0, radius);
        float[] kernel = new float[radius + 1];
        if (radius == 0) {
            kernel[0] = 1.0F;
            return kernel;
        }

        float sigma = radius / 2.0F;
        float sum = 0.0F;
        for (int i = 0; i <= radius; i++) {
            float multiplier = i / sigma;
            kernel[i] = 1.0F / (Math.abs(sigma) * 2.50662827463F) * (float) Math.exp(-0.5F * multiplier * multiplier);
            sum += i > 0 ? kernel[i] * 2.0F : kernel[0];
        }

        for (int i = 0; i <= radius; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }
}
