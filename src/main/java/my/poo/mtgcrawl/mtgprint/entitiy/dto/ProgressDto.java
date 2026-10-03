package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;

/**
 * Ein Fortschrittsschritt eines PDF-Jobs. Das Frontend pollt die komplette
 * Historie über GET /api/progress/{id} und zeigt pro Kategorie den letzten Eintrag.
 */
@Data
public class ProgressDto {
    private String progressId;
    private Category category;
    private String message = "";
    private int current;
    private int total;
    private Integer pageCount;
    private long fileSize;
    private boolean error;

    public enum Category {
        queued,
        download,
        build,
        finishing
    }

    public static ProgressDto of(String progressId, Category category, String message, int current, int total) {
        ProgressDto dto = new ProgressDto();
        dto.setProgressId(progressId);
        dto.setCategory(category);
        dto.setMessage(message);
        dto.setCurrent(current);
        dto.setTotal(total);
        return dto;
    }
}
