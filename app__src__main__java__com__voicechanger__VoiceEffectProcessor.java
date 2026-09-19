package com.voicechanger;

public class VoiceEffectProcessor {
    private final int sampleRate;
    private float pitchFactor = 1.0f;
    private float speedFactor = 1.0f;
    private float volumeLevel = 0.7f;
    private int effectType = 0;

    private short[] echoBuffer;
    private int echoBufferIndex = 0;
    private final int ECHO_DELAY_SAMPLES = 22050; // 0.5 sec at 44.1kHz

    public VoiceEffectProcessor(int sampleRate) {
        this.sampleRate = sampleRate;
        this.echoBuffer = new short[ECHO_DELAY_SAMPLES];
    }

    public short[] processAudio(short[] audioBuffer, int length) {
        short[] output = new short[length];
        System.arraycopy(audioBuffer, 0, output, 0, length);

        // Apply pitch
        if (Math.abs(pitchFactor - 1.0f) > 0.01f) {
            output = applyPitch(output, length);
        }

        // Apply speed
        if (Math.abs(speedFactor - 1.0f) > 0.01f) {
            output = applySpeed(output, length);
        }

        // Apply effect
        switch (effectType) {
            case 1: // Robot
                output = applyRobot(output, length);
                break;
            case 2: // Helium
                output = applyHelium(output, length);
                break;
            case 3: // Deep
                output = applyDeep(output, length);
                break;
            case 4: // Echo
                output = applyEcho(output, length);
                break;
            case 5: // Reverb
                output = applyReverb(output, length);
                break;
        }

        // Apply volume
        output = applyVolume(output, length);

        return output;
    }

    private short[] applyPitch(short[] audio, int length) {
        short[] pitched = new short[length];

        if (pitchFactor > 1.0f) {
            int idx = 0;
            for (int i = 0; i < length; i++) {
                int sampleIdx = (int) (i * pitchFactor);
                if (sampleIdx < length) {
                    pitched[i] = audio[sampleIdx];
                } else {
                    pitched[i] = audio[length - 1];
                }
            }
        } else {
            for (int i = 0; i < length; i++) {
                float sourceIdx = i / pitchFactor;
                int floor = (int) sourceIdx;
                float frac = sourceIdx - floor;

                if (floor < length - 1) {
                    pitched[i] = (short) (audio[floor] * (1 - frac) + audio[floor + 1] * frac);
                } else if (floor < length) {
                    pitched[i] = audio[floor];
                }
            }
        }

        return pitched;
    }

    private short[] applySpeed(short[] audio, int length) {
        int newLength = Math.min((int) (length / speedFactor), length);
        short[] speedAudio = new short[length];

        for (int i = 0; i < newLength; i++) {
            float sourceIdx = i * speedFactor;
            int floor = (int) sourceIdx;
            float frac = sourceIdx - floor;

            if (floor < length - 1) {
                speedAudio[i] = (short) (audio[floor] * (1 - frac) + audio[floor + 1] * frac);
            } else if (floor < length) {
                speedAudio[i] = audio[floor];
            }
        }

        return speedAudio;
    }

    private short[] applyRobot(short[] audio, int length) {
        short[] robot = new short[length];

        for (int i = 0; i < length; i++) {
            int cyclePos = (i % (sampleRate / 200));
            short amp = audio[i];

            if (cyclePos < (sampleRate / 400)) {
                robot[i] = amp;
            } else {
                robot[i] = (short) -amp;
            }
        }

        return robot;
    }

    private short[] applyHelium(short[] audio, int length) {
        short[] helium = new short[length];

        for (int i = 0; i < length; i++) {
            int idx = (int) (i * 1.5f);
            if (idx < length) {
                helium[i] = (short) (audio[idx] * 0.8f);
            }
        }

        return helium;
    }

    private short[] applyDeep(short[] audio, int length) {
        short[] deep = new short[length];

        for (int i = 0; i < length; i++) {
            int idx = (int) (i * 0.65f);
            if (idx < length) {
                short base = audio[idx];
                short harmonic = (short) (audio[Math.max(0, idx - 50)] * 0.4f);
                deep[i] = (short) ((base + harmonic) * 0.85f);
            }
        }

        return deep;
    }

    private short[] applyEcho(short[] audio, int length) {
        short[] echoed = new short[length];

        for (int i = 0; i < length; i++) {
            short current = audio[i];
            short echo = echoBuffer[echoBufferIndex];

            echoed[i] = (short) (current * 0.8f + echo * 0.3f);

            echoBuffer[echoBufferIndex] = current;
            echoBufferIndex = (echoBufferIndex + 1) % ECHO_DELAY_SAMPLES;
        }

        return echoed;
    }

    private short[] applyReverb(short[] audio, int length) {
        short[] reverb = new short[length];
        int[] delays = {100, 200, 300};
        float[] gains = {0.4f, 0.3f, 0.2f};

        for (int i = 0; i < length; i++) {
            float sample = audio[i];

            for (int j = 0; j < delays.length; j++) {
                int delayIdx = i - delays[j];
                if (delayIdx >= 0) {
                    sample += audio[delayIdx] * gains[j];
                }
            }

            reverb[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample * 0.7f));
        }

        return reverb;
    }

    private short[] applyVolume(short[] audio, int length) {
        short[] volumed = new short[length];

        for (int i = 0; i < length; i++) {
            volumed[i] = (short) (audio[i] * volumeLevel);
        }

        return volumed;
    }

    public void setPitch(float pitch) {
        this.pitchFactor = Math.max(0.5f, Math.min(2.5f, pitch));
    }

    public void setSpeed(float speed) {
        this.speedFactor = Math.max(0.5f, Math.min(2.5f, speed));
    }

    public void setVolume(float volume) {
        this.volumeLevel = Math.max(0f, Math.min(1f, volume));
    }

    public void setEffect(int effect) {
        this.effectType = effect;
    }
}
