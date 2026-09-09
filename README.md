# FeeL Backend Server

전북대학교 공과대학 FeeL 학생회 백엔드 서버 - 공지사항 및 갤러리 관리 시스템

## 기술 스택

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA with Hibernate
- MySQL 8.0 (배포)
- Docker & Docker Compose
- Nginx (Reverse Proxy)
- Certbot (SSL/TLS)
- Lombok

## 개발 환경 실행

### 1. H2 데이터베이스 사용 (로컬 개발)

```bash
# Maven을 사용한 실행
./mvnw spring-boot:run

# 또는 IDE에서 BackendApplication.java 실행
```

서버 실행 후:
- API Base URL: http://localhost:8080
  - 공지사항: http://localhost:8080/api/notices
  - 갤러리: http://localhost:8080/api/gallery
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/feeldb`
  - Username: `sa`
  - Password: (비워두기)

### 2. Docker를 사용한 실행

```bash
# 환경 변수 설정
cp .env.example .env
# .env 파일 수정하여 DB_USERNAME, DB_PASSWORD 설정

# Docker Compose로 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f backend

# 개별 서비스 상태 확인
docker-compose ps

# 종료
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```

**배포 환경 구성:**
- **Nginx**: Reverse Proxy (포트 80, 443)
- **Backend**: Spring Boot 애플리케이션 (내부 포트 8080)
- **MySQL**: 데이터베이스 (포트 3307 → 3306)
- **Certbot**: SSL/TLS 인증서 자동 갱신

## API 엔드포인트

### 인증 API (`/api/auth`)

Google 소셜 로그인(ID 토큰) + 관리자 비밀번호 로그인.

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/auth/google` | Google ID 토큰 로그인 (`@jbnu.ac.kr`만). body: `{ "idToken": "..." }` |
| POST | `/api/auth/signup` | 신규 가입(닉네임). body: `{ "idToken": "...", "nickname": "..." }` |
| POST | `/api/auth/login` | 관리자 username/password 로그인 |
| GET | `/api/auth/verify` | `Authorization: Bearer <JWT>` 검증 |
| POST | `/api/auth/logout` | 로그아웃 (클라이언트 토큰 삭제용, 서버는 no-op) |

**Google 로그인 흐름**
1. 프론트에서 Google Identity Services로 ID 토큰 발급
2. `POST /api/auth/google` — 기존 유저면 `token` 반환, 신규면 `needSignup: true` + `email`
3. 신규는 닉네임 입력 후 `POST /api/auth/signup`
4. 이후 API 호출 시 `Authorization: Bearer <token>`

환경변수 `GOOGLE_CLIENT_ID`는 프론트 Client ID와 동일해야 합니다. (`.env.example` 참고)

### 공지사항 API (`/api/notices`)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/notices` | 공지사항 생성 (이미지 업로드 선택) |
| POST | `/api/notices/batch` | 여러 공지사항 일괄 생성 |
| GET | `/api/notices` | 전체 공지사항 조회 (페이징, 카테고리 필터) |
| GET | `/api/notices/{id}` | 특정 공지사항 조회 (조회수 증가) |
| GET | `/api/notices/pinned` | 고정 공지사항 조회 |
| GET | `/api/notices/category/{category}` | 카테고리별 조회 |
| GET | `/api/notices/search?keyword=검색어` | 검색 (카테고리 필터 지원) |
| PUT | `/api/notices/{id}` | 공지사항 수정 (이미지 업로드 선택) |
| DELETE | `/api/notices/{id}` | 공지사항 삭제 |

### 갤러리 API (`/api/gallery`)

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/gallery` | 갤러리 생성 (이미지 업로드 필수) |
| GET | `/api/gallery` | 전체 갤러리 조회 (페이징, 카테고리 필터) |
| GET | `/api/gallery/recent` | 최근 갤러리 조회 (최대 10개) |
| GET | `/api/gallery/{id}` | 특정 갤러리 조회 (조회수 증가) |
| GET | `/api/gallery/search?keyword=검색어` | 검색 (카테고리 필터 지원) |
| PUT | `/api/gallery/{id}` | 갤러리 수정 (이미지 업로드 선택) |
| DELETE | `/api/gallery/{id}` | 갤러리 삭제 |

## 배포

### Docker를 이용한 배포

```bash
# 프로덕션 환경으로 실행
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 백엔드 로그 확인
docker-compose logs -f backend

# Nginx 로그 확인
docker-compose logs -f nginx

# 모든 서비스 재시작
docker-compose restart
```

### SSL/TLS 인증서 설정

```bash
# SSL 인증서 발급 (최초 1회)
docker-compose run --rm certbot certonly --webroot \
  --webroot-path=/var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d yourdomain.com

# 인증서 자동 갱신은 Certbot 컨테이너가 12시간마다 자동 실행
```

### 프로필 설정

- **개발**: `application.properties` (H2)
- **배포**: `application-prod.properties` (MySQL)

환경 변수로 프로필 전환:
```bash
export SPRING_PROFILES_ACTIVE=prod
```


```

## 프로젝트 구조

```
src/main/java/com/feel/backend/
├── BackendApplication.java
├── config/
│   ├── WebConfig.java           # CORS 설정
│   └── FileUploadConfig.java    # 파일 업로드 설정
├── controller/
│   ├── NoticeController.java    # 공지사항 API
│   └── GalleryController.java   # 갤러리 API
├── dto/
│   ├── NoticeRequestDto.java
│   ├── NoticeResponseDto.java
│   ├── GalleryRequestDto.java
│   └── GalleryResponseDto.java
├── entity/
│   ├── Notice.java              # 공지사항 엔티티
│   ├── NoticeCategory.java      # 공지사항 카테고리 Enum
│   └── Gallery.java             # 갤러리 엔티티
├── repository/
│   ├── NoticeRepository.java
│   └── GalleryRepository.java
└── service/
    ├── NoticeService.java       # 공지사항 비즈니스 로직
    ├── GalleryService.java      # 갤러리 비즈니스 로직
    └── FileStorageService.java  # 파일 업로드/삭제 로직
```

## CORS 설정

다음 Origin에서 접근 허용:
- http://localhost:3000 (React 개발 서버)
- https://m-se0k.github.io (배포된 프론트엔드)

새로운 프론트엔드 Origin을 추가하려면 `config/WebConfig.java` 또는 각 컨트롤러의 `@CrossOrigin` 어노테이션을 수정하세요.

## 파일 업로드

- **업로드 경로**: `uploads/` 디렉토리
- **지원 형식**: 이미지 파일 (JPEG, PNG, GIF 등)
- **최대 크기**: application.properties에서 설정 가능
- **공지사항**: 이미지 업로드 선택 사항
- **갤러리**: 이미지 업로드 필수

업로드된 파일은 `/uploads/{filename}` 경로로 접근 가능합니다.

## 빌드 및 테스트

```bash
# 클린 및 컴파일
./mvnw clean compile

# 테스트 실행
./mvnw test

# 패키징
./mvnw package

# 테스트 스킵하고 빌드
./mvnw package -DskipTests
```



## 주요 기능

### 공지사항 관리
- 공지사항 CRUD 작업
- 이미지 업로드 지원 (선택)
- 고정 공지사항 기능
- 카테고리별 분류 및 필터링
- 페이징 및 검색 기능
- 조회수 자동 추적
- 일괄 생성 기능

### 갤러리 관리
- 갤러리 CRUD 작업
- 이미지 업로드 지원 (필수)
- 카테고리별 분류 및 필터링
- 페이징 및 검색 기능
- 조회수 자동 추적
- 최근 갤러리 조회 기능

## 중요 참고사항

- **H2 파일 위치**: `./data/feeldb` (자동 생성됨)
- **포트 충돌**: 8080, 80, 443, 3307 포트가 사용 중이면 `docker-compose.yml` 수정 필요
- **Windows 호환성**: Windows CMD에서는 `mvnw.cmd` 사용 (Git Bash는 `./mvnw` 지원)
- **Lombok**: IDE에 Lombok 플러그인이 설치되어 있어야 어노테이션이 정상 처리됨
- **트랜잭션 관리**: 데이터를 수정하는 Service 메서드는 반드시 `@Transactional` 어노테이션 필요
- **파일 업로드**: `uploads/` 디렉토리는 자동으로 생성되며, Docker 볼륨으로 관리됨

## 라이선스

MIT
