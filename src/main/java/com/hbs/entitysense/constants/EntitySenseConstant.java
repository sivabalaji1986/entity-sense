package com.hbs.entitysense.constants;

public class EntitySenseConstant {

    public static final String OLLAMA_URL       = "http://localhost:11434";

    public static final String OLLAMA_EMBEDDINGS_URL       = "/api/embeddings";

    public static final String OLLAMA_EMBEDDINGS_REQ_MODEL_VALUE      = "nomic-embed-text";

    public static final String OPEN_API_SPEC_TITLE       = "EntitySense API";

    public static final String OPEN_API_SPEC_DESCRIPTION      = "Spring Boot API for Detecting sanctioned or mule entities during fund transfers";

    public static final String OPEN_API_SPEC_VERSION      = "1.0.0";

    public static final String OLLAMA_EMBEDDINGS_REQ_MODEL_KEY      = "model";

    public static final String OLLAMA_EMBEDDINGS_REQ_PROMPT_KEY      = "prompt";

    public static final String OLLAMA_CONTENT_TYPE_KEY      = "Content-Type";

    public static final String OLLAMA_CONTENT_TYPE_VALUE      = "application/json";

    public static final int OLLAMA_TIMEOUT_VALUE      = 10;

    public static final String PAYMENT_STATUS_ALLOW       = "ALLOW";

    public static final String PAYMENT_STATUS_BLOCK       = "BLOCK";

}
