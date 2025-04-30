# 5-team-oishikunare-gangazido-be
# 🐶 강아지도 Backend 

반려견 산책 추천 및 마커 관리 서비스를 위한 백엔드 API입니다.

---

## 🛠️ 사용 기술

| 구분 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Build Tool | Gradle 8 |
| Database | MariaDB |
| Cache & Session | Redis |
| Auth | 쿠키 + 세션 기반 인증 |
| Password Hash | Argon2PasswordEncoder |
| Rate Limiting | Bucket4j |
| Mail | Spring Mail (Gmail SMTP) |
| Image | S3 + CloudFront + Presigned URL |
| AI | OpenAI GPT 3.5 Turbo |
| Weather API | OpenWeatherMap |
| Infra | Docker, EC2, CloudFront |
| Monitoring | Prometheus + Grafana |

---

## 🔐 인증 및 보안

- ✅ 쿠키 + 세션 기반 로그인 유지
- ✅ SameSite 설정으로 서브도메인 쿠키 공유
- ✅ Argon2 사용 (메모리·시간 조절 가능, bcrypt보다 안전)
- ✅ Redis로 세션 중앙 관리 → 블루그린 배포 대응
- ✅ Bucket4j로 IP 기반 API Rate Limit 적용 (예: 로그인 1분 30회 제한)

---

## 📧 이메일 인증 로직

- 6자리 난수 생성 → Redis에 5분 TTL로 저장
- Gmail SMTP 서버로 인증 메일 전송
- 사용자 입력값을 Redis에 저장된 값과 비교
- 인증 성공 후 회원가입 완료 처리

---

## 🖼️ 이미지 업로드 로직

- 프론트에서 Presigned URL 요청 → 백엔드에서 발급
- 사용자는 직접 S3에 업로드 (PUT)
- 업로드된 S3 키를 백엔드에 전달하여 저장
- CloudFront를 통해 미리보기 가능 (빠른 캐시 응답)

| 상태 | 프론트 요청 | 백엔드 처리 |
|------|-------------|-------------|
| 삭제 | `profileImage = null` | 기존 이미지 키 삭제 |
| 유지 | 이미지 전송 생략 | 기존 이미지 유지 |
| 변경 | File → S3 키 전송 | 새 이미지로 갱신 |

---

## 🧪 성능 테스트

- YCSB 기반 Redis/Memcached 성능 비교
- Workload A 및 커스텀 workload 사용
- 주요 메트릭: `commands_processed`, `memory_used_bytes`, `CPU 사용률`

---

## 👨‍👩‍👧‍👦 담당 역할
| 이름                 | 역할 요약                                                                 |
|----------------------|--------------------------------------------------------------------------|
| ✨ Rachel.Kim (김명빈) | - 반려견 등록/수정 API<br>- Presigned URL 기반 이미지 업로드<br>- CloudFront 적용<br>- Bucket4j RateLimit |
| ✨ Jerry.Park (박경승) | - 지도 및 마커 등록 API<br>- Bucket4j RateLimit 적용                       |
| ✨ Jack.Lee (이세현)   | - 로그인/회원가입 API<br>- Argon2 암호화<br>- 내 정보 수정<br>- Redis 세션 관리<br>- RateLimit |
| ✨ Yeoni.Lee (이주연)  | - Chat 기능 API 구현<br>- GPT 프롬프트 설계 및 연동<br>- 이메일 인증 구현<br>- Redis & Memcached 성능 테스트 |


# 🐶 강아지도 DevOps

## 담당자 소개
| ✨ Zino.Heo (허진호)  | - 데브옵스 |

## 🛠️ 사용 기술

## 아키텍처 구조

## 모니터링 툴

