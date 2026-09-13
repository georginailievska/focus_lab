package mk.focuslab.dto;

public record AttachmentResponse(
        Long id,
        String url,
        String filename,
        String contentType,
        long sizeBytes,
        boolean image
) {
}
