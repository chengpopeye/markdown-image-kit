package info.dong4j.idea.plugin.sdk.qcloud.cos.transfer;

import info.dong4j.idea.plugin.sdk.qcloud.cos.COS;
import info.dong4j.idea.plugin.sdk.qcloud.cos.event.ProgressListenerChain;
import info.dong4j.idea.plugin.sdk.qcloud.cos.exception.CosClientException;
import info.dong4j.idea.plugin.sdk.qcloud.cos.model.CompleteMultipartUploadRequest;
import info.dong4j.idea.plugin.sdk.qcloud.cos.model.CompleteMultipartUploadResult;
import info.dong4j.idea.plugin.sdk.qcloud.cos.model.PartETag;
import info.dong4j.idea.plugin.sdk.qcloud.cos.model.PutObjectRequest;
import info.dong4j.idea.plugin.sdk.qcloud.cos.model.UploadResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Initiates a complete multi-part upload request for a
 * TransferManager multi-part parallel upload.
 */
public class CompleteMultipartUpload implements Callable<UploadResult> {

    /** The upload id associated with the multi-part upload. */
    private final String uploadId;

    /**
     * The reference to underlying Qcloud COS client to be used for initiating
     * requests to Qcloud COS.
     */
    private final COS cos;

    /** The reference to the request initiated by the user. */
    private final PutObjectRequest origReq;

    /** The futures of threads that upload individual parts. */
    private final List<Future<PartETag>> futures;

    /**
     * The eTags of the parts that had been successfully uploaded before
     * resuming a paused upload.
     */
    private final List<PartETag> eTagsBeforeResume;

    /** The monitor to which the upload progress has to be communicated. */
    private final UploadMonitor monitor;

    /** The listener where progress of the upload needs to be published. */
    private final ProgressListenerChain listener;

    public CompleteMultipartUpload(String uploadId, COS cos,
            PutObjectRequest putObjectRequest, List<Future<PartETag>> futures,
            List<PartETag> eTagsBeforeResume, ProgressListenerChain progressListenerChain,
            UploadMonitor monitor) {
        this.uploadId = uploadId;
        this.cos = cos;
        this.origReq = putObjectRequest;
        this.futures = futures;
        this.eTagsBeforeResume = eTagsBeforeResume;
        this.listener = progressListenerChain;
        this.monitor = monitor;
    }

    @Override
    public UploadResult call() throws Exception {
        CompleteMultipartUploadResult res;

        try {
            CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(
                    origReq.getBucketName(), origReq.getKey(), uploadId,
                    collectPartETags())
                .withGeneralProgressListener(origReq.getGeneralProgressListener())
                ;
            res = cos.completeMultipartUpload(req);
        } catch (Exception e) {
            monitor.uploadFailed();
            throw e;
        }

        UploadResult uploadResult = new UploadResult();
        uploadResult.setBucketName(origReq
                .getBucketName());
        uploadResult.setKey(origReq.getKey());
        uploadResult.setETag(res.getETag());
        uploadResult.setVersionId(res.getVersionId());
        uploadResult.setRequestId(res.getRequestId());
        uploadResult.setDateStr(res.getDateStr());

        monitor.uploadComplete();

        return uploadResult;
    }

    /**
     * Collects the Part ETags for initiating the complete multi-part upload
     * request. This is blocking as it waits until all the upload part threads
     * complete.
     */
    private List<PartETag> collectPartETags() {

        final List<PartETag> partETags = new ArrayList<PartETag>();
        partETags.addAll(eTagsBeforeResume);
        for (Future<PartETag> future : futures) {
            try {
                partETags.add(future.get());
            } catch (Exception e) {
                throw new CosClientException(
                        "Unable to complete multi-part upload. Individual part upload failed : "
                                + e.getCause().getMessage(), e.getCause());
            }
        }
        return partETags;
    }
}
