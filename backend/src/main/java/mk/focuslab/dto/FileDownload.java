package mk.focuslab.dto;

/** Датотека како што се служи кон прелистувачот. */
public record FileDownload(String filename, String contentType, boolean inline, byte[] data) {
}
