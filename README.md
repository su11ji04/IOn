<BACK-END>
BACK-END 역할
- 사용자 관리 및 인증
- AI 분석 결과 저장 및 조회
- Voice Report / Workbook / Chatbot API 제공
- RDS(MySQL) 기반 데이터 영속화
- Python AI 마이크로서비스 연동
  
capstone
├── chatbot           # 육아 챗봇
├── voicereport       # 보이스리포트
├── workbook          # 워크북
├── user              # 사용자 프로필
├── home              # 홈 화면 관련 API
├── web               # 공통 Web 설정
├── common            # 공통 유틸
├── config            # Security, WebClient, JPA 설정
└── support           # 공통 지원 클래스

기술 스택
- Java 21
- Spring Boot 3.x
- Spring Data JPA (Hibernate)
- MySQL (AWS RDS)
- WebClient (외부 AI 서버 연동)
- Gradle
  
BACK-END BULID & RUN TEST
1) Gradle 빌드
2) http://localhost:8080로 실행
