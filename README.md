# routereceipt

## 프로젝트 소개

routereceipt는 영수증 이미지를 업로드하면 OCR로 텍스트를 추출하고, AI 분석을 통해 소비 패턴을 분류·시각화하는 웹 서비스입니다. 5인 팀 프로젝트로 진행되었으며, 사용자가 손쉽게 지출 내역을 확인하고 소비 성향을 파악할 수 있도록 설계되었습니다.

## 주요 기능

- 영수증 이미지 업로드 및 OCR 기반 텍스트 추출
- AI 기반 소비 항목 분류 및 패턴 분석
- 소비 내역 시각화 및 조회 기능
- 사용자별 지출 통계 확인
- 비동기 처리 기반의 빠른 응답 경험 제공

## 기술 스택

- Java
- Spring Boot
- eGovFrame
- JSP
- Oracle DB
- MyBatis
- Spring Data JPA
- Redis Stream
- Podman

## 아키텍처

사용자가 영수증을 업로드하면 Clova OCR를 통해 텍스트를 추출하고, 해당 데이터를 Redis Stream을 통해 비동기적으로 전달합니다. 이후 FastAPI 기반 분석 서버가 소비 패턴을 분석한 뒤 결과를 저장하고 시각화합니다.

```text
영수증 업로드
  → Clova OCR
  → Redis Stream
  → FastAPI 분석 서버
  → 결과 저장 / 시각화
```

## 핵심 성과

- Redis Stream 기반 비동기 파이프라인 도입으로 응답 시간을 약 20~30초에서 4~5초 수준으로 단축
- 약 80% 수준의 처리 속도 개선 달성
- Podman 컨테이너 기반 배포 구성을 적용하여 서비스 운영 환경을 구성

## 프로젝트 구조

```text
routereceipt/
├── src/main/java          # Spring Boot 애플리케이션
├── src/main/resources     # 설정 파일, Mapper, 템플릿, 프롬프트
├── fastapi-ai             # FastAPI 기반 분석 서버
├── docker-compose.yml     # 컨테이너 구성
├── dockerfile             # 애플리케이션 이미지 빌드
└── build.gradle           # Gradle 빌드 설정
```

## 실행 방법

### 1. Podman 기반 실행

```bash
podman compose up -d
```

### 2. 로컬 실행

```bash
./gradlew bootRun
```

FastAPI 서버는 별도로 실행해야 하며, 필요 시 아래와 같이 실행할 수 있습니다.

```bash
cd fastapi-ai
pip install -r requirements.txt
python main.py
```

```

## 트러블슈팅

### 1. Redis Stream 비동기 설정 파일 누락으로 인한 간헐적 실패

- 문제
  - Redis Stream 기반 비동기 처리 과정에서 설정 파일 누락으로 인해 간헐적으로 처리 실패가 발생했습니다.
- 원인
  - 비동기 파이프라인 구성에 필요한 설정이 누락되어, 특정 상황에서 메시지 전달 및 처리 흐름이 중단되었습니다.
- 해결
  - 원인을 3~6일에 걸쳐 추적한 결과, 누락된 설정 파일과 연결 구성을 확인하고 정상 동작하도록 보완했습니다.
  - 이후 비동기 처리 흐름이 안정적으로 동작하도록 구성 파일을 정리하고 재검증했습니다.

### 2. MyBatis → JPA 마이그레이션 시 save()가 INSERT 대신 UPDATE로 동작한 문제

- 문제
  - MyBatis 기반 구조에서 JPA로 마이그레이션한 뒤, 엔티티 저장 시 `save()`가 기대와 다르게 UPDATE로 동작했습니다.
- 원인
  - 새 엔티티인지 여부를 판단하는 로직이 명확하지 않아, JPA가 기존 엔티티로 오인하는 문제가 있었습니다.
- 해결
  - `Persistable` 인터페이스를 구현하고 `isNew()` 메서드를 명시적으로 정의하여, 신규 엔티티는 INSERT, 기존 엔티티는 UPDATE로 분기되도록 수정했습니다.

## 개발 포인트

- 비동기 처리와 응답 성능 최적화에 중점을 두었습니다.
- 기존 MyBatis 기반 구조에서 JPA로의 전환 과정에서 데이터 저장 로직 안정성을 확보했습니다.
- 컨테이너 기반 배포 환경을 고려한 구성으로 확장성과 운영 편의성을 높였습니다.
```
