package io.roach.batch.web;

public abstract class LinkRels {
    public static final String DOWNLOAD_REL = "download";

    public static final String ACTUATOR_REL = "actuator";

    // IANA standard link relations:
    // http://www.iana.org/assignments/link-relations/link-relations.xhtml

    public static final String CURIE_NAMESPACE = "batch";

    public static final String CURIE_PREFIX = CURIE_NAMESPACE + ":";

    private LinkRels() {
    }

    public static String withCurie(String rel) {
        return CURIE_NAMESPACE + ":" + rel;
    }
}
