package me._hanho.ultary.domain.admin.dto.response;

import java.time.LocalDateTime;
import lombok.Data;
import me._hanho.ultary.domain.file.dto.response.FileSummaryResponse;

@Data
public class AdminUserResponse {
    private Long userNo;
    private String nickname;
    private java.util.List<String> petNames = java.util.List.of();
    private String name;
    private String status;
    private String maskedEmail;
    private String maskedPhone;
    private LocalDateTime createdAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime withdrawalCompletedAt;
    private int petCount;
    private int feedCount;
    private int reportCount;
    private boolean googleLinked;
    private boolean kakaoLinked;
    private Long profileFileId;
    private FileSummaryResponse profileFile;
}
