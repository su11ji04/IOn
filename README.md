## BACK-END<br>

<BACK-END 역할>
- 사용자 관리 및 인증<br>
- AI 분석 결과 저장 및 조회<br>
- Voice Report / Workbook / Chatbot API 제공<br>
- RDS(MySQL) 기반 데이터 영속화<br>
- Python AI 마이크로서비스 연동<br><br>

<폴더 구조><br>
capstone<br>
├── chatbot           # 육아 챗봇<br>
├── voicereport       # 보이스리포트<br>
├── workbook          # 워크북<br>
├── user              # 사용자 프로필<br>
├── home              # 홈 화면 관련 API<br>
├── web               # 공통 Web 설정<br>
├── common            # 공통 유틸<br>
├── config            # Security, WebClient, JPA 설정<br>
└── support           # 공통 지원 클래스<br><br>

<기술 스택>
- Java 21<br>
- Spring Boot 3.x<br>
- Spring Data JPA (Hibernate)<br>
- MySQL (AWS RDS)<br>
- WebClient (외부 AI 서버 연동)<br>
- Gradle<br><br>

<실행 환경>
- Java 21<br>
- Gradle 8.x<br>
- MySQL 8.x<br><br>
  
<BACK-END BULID & RUN TEST>
- Gradle 빌드 -> http://localhost:8080로 실행<br>
