package mk.focuslab.dto;

/** Бајтите на профилната слика како што се служат кон прелистувачот. */
public record AvatarData(String contentType, byte[] data) {
}
