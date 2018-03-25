package io.github.introml.activityrecognition;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


public class TensorFlowClassifier {
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TensorFlowInferenceInterface inferenceInterface;
    //private static final String MODEL_FILE = "file:///android_asset/frozen_model.pb";
    //private static final String MODEL_FILE = "file:///android_asset/frozen_har.pb";
    private static final String MODEL_FILE = "file:///android_asset/2layer64unit.pb";
    private static final String INPUT_NODE = "input";
    private static final String[] OUTPUT_NODES = {"output"};
    private static final String OUTPUT_NODE = "output";
//    private static final String[] OUTPUT_NODES = {"y_"};
//    private static final String OUTPUT_NODE = "y_";
    private static final long[] INPUT_SIZE = {1, 128, 9};
    private static final int OUTPUT_SIZE = 6;

    public TensorFlowClassifier(final Context context) {
        inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
    }

    public float[] predictProbabilities(float[] data) {
        float[] result = new float[OUTPUT_SIZE];
        //int[] result = new int[1024];
        inferenceInterface.feed(INPUT_NODE, data, INPUT_SIZE);
        inferenceInterface.run(OUTPUT_NODES);
        inferenceInterface.fetch(OUTPUT_NODE, result);
        return result;
    }
}
