package info.dong4j.idea.plugin.sdk.qcloud.cos.model;

import java.io.*;

import static info.dong4j.idea.plugin.sdk.qcloud.cos.model.InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX;
import static info.dong4j.idea.plugin.sdk.qcloud.cos.model.InstructionFileId.DOT;

public class COSObjectId implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String bucket;
    private final String key;
    /**
     * Optional and applicable only for get operation.
     */
    private final String versionId;

    public COSObjectId(String bucket, String key) {
        this(bucket, key, null);
    }

    /**
     * @param bucket the COS bucket name which must not be null
     * @param key the COS key name which must not be null
     * @param versionId optional version id
     */
    public COSObjectId(String bucket, String key, String versionId) {
        if (bucket == null || key == null)
            throw new IllegalArgumentException("bucket and key must be specified");
        this.bucket = bucket;
        this.key = key;
        this.versionId = versionId;
    }

    /**
     * @param builder must not be null.
     */
    public COSObjectId(COSObjectIdBuilder builder) {
        this.bucket = builder.getBucket();
        this.key = builder.getKey();
        this.versionId = builder.getVersionId();
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    /**
     * Returns the version id which is optionally applicable for COS get (but not put) operations.
     */
    public String getVersionId() {
        return versionId;
    }
    /**
     * Returns the instruction file id of the default instruction file.
     */
    public InstructionFileId instructionFileId() {
        return instructionFileId(null);
    }

    /**
     * Returns the instruction file id of an instruction file with the given
     * suffix.
     */
    public InstructionFileId instructionFileId(String suffix) {
        String ifileKey = key + DOT;
        ifileKey += (suffix == null || suffix.trim().length() == 0)
                  ? DEFAULT_INSTRUCTION_FILE_SUFFIX
                  : suffix
                  ;
         return new InstructionFileId(bucket, ifileKey, versionId);
    }

    @Override
    public String toString() {
        return "bucket: " + bucket + ", key: " + key + ", versionId: "
                + versionId;
    }
}
