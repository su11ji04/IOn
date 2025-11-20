package capstone.voicereport.dto;

import capstone.user.dto.UserRegisterDto;

import java.util.List;
import java.util.Map;
import lombok.Data;
import java.util.Map;

@Data
public class VoiceReportSummaryDto {
    private String momentEmotion; //나의 감정 분석
    private Integer parentFrequency;  //발화 빈도 분석_부모
    private Integer kidFrequency; //발화 빈도 분석_아이
    private String overallFeedback; //대체 표현 제안
}

