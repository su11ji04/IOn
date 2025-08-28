-- 메인 테이블: voice_report
CREATE TABLE IF NOT EXISTS voice_report (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            user_id BIGINT,

                                            audio_original_name VARCHAR(255),
    audio_size BIGINT,
    audio_path VARCHAR(500),

    sub_title VARCHAR(200),
    report_day VARCHAR(30),

    conversation_summary VARCHAR(4000),
    length_seconds INTEGER,
    overall_feedback VARCHAR(2000),

    -- @Embedded Frequency
    parent_frequency INTEGER,
    kid_frequency INTEGER,
    frequency_feedback VARCHAR(1000),

    -- @Embedded Expression
    parent_expression VARCHAR(1000),
    kid_expression VARCHAR(1000),
    parent_conditions VARCHAR(1000),
    kid_conditions VARCHAR(1000),
    expression_feedback VARCHAR(1000),

    -- Emotion feedback text (timeline은 별도 테이블)
    emotion_feedback VARCHAR(2000),

    kid_attitude VARCHAR(500),

    pattern VARCHAR(1000),
    strength VARCHAR(1000),

    created_at TIMESTAMP,
    updated_at TIMESTAMP
    );

-- 감정 타임라인: @ElementCollection
-- 테이블명: voice_report_emotion_timeline, FK: report_id
CREATE TABLE IF NOT EXISTS voice_report_emotion_timeline (
                                                             report_id BIGINT NOT NULL,
                                                             time VARCHAR(30),
    moment_emotion VARCHAR(100)
    );

-- 말투 교체 제안: @ElementCollection
-- 테이블명: voice_report_change_proposal, FK: report_id
CREATE TABLE IF NOT EXISTS voice_report_change_proposal (
                                                            report_id BIGINT NOT NULL,
                                                            existing_expression VARCHAR(1000),
    proposal_expression VARCHAR(1000)
    );

-- (선택) 조회 최적화용 인덱스
CREATE INDEX IF NOT EXISTS idx_voice_report_user_id ON voice_report(user_id);
CREATE INDEX IF NOT EXISTS idx_vr_emotion_report_id ON voice_report_emotion_timeline(report_id);
CREATE INDEX IF NOT EXISTS idx_vr_change_report_id ON voice_report_change_proposal(report_id);
