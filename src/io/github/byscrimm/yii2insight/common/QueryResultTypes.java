package io.github.byscrimm.yii2insight.common;

import java.util.List;
import java.util.Set;

/** Result types of ActiveQuery terminal methods for a model class. */
public final class QueryResultTypes {
    private static final Set<String> METHODS = Set.of("one", "all", "each", "batch");
    private static final String BATCH_RESULT = "\\yii\\db\\BatchQueryResult";

    private QueryResultTypes() {
    }

    public static boolean isResultMethod(String name) {
        return name != null && METHODS.contains(name);
    }

    /** Returns PHP types for a provider signature ending with {@code .method}. */
    public static List<String> types(String modelFqn, String signature) {
        if (modelFqn == null || signature == null) {
            return List.of();
        }
        if (signature.endsWith(".one")) {
            return List.of(modelFqn, "null");
        }
        if (signature.endsWith(".all")) {
            return List.of(modelFqn + "[]");
        }
        // foreach over BatchQueryResult yields models for each() and model arrays for batch().
        if (signature.endsWith(".each")) {
            return List.of(BATCH_RESULT, modelFqn + "[]");
        }
        if (signature.endsWith(".batch")) {
            return List.of(BATCH_RESULT, modelFqn + "[][]");
        }
        return List.of();
    }
}
