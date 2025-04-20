1. **Docker 설치 확인**
``` bash
docker --version
```
1. **RabbitMQ 이미지 가져오기** (management 플러그인 포함된 버전)
``` bash
docker pull rabbitmq:3-management
```
1. **Docker 컨테이너 실행**
``` bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -p 1883:1883 \
  -p 15675:15675 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  rabbitmq:3-management
```
포트 설명:
- 5672: AMQP 프로토콜
- 15672: 관리자 웹 인터페이스
- 1883: MQTT 프로토콜
- 15675: MQTT over WebSocket

1. **MQTT 플러그인 활성화**
``` bash
# 컨테이너 내부 접속
docker exec -it rabbitmq bash

# MQTT 플러그인 활성화
rabbitmq-plugins enable rabbitmq_mqtt
rabbitmq-plugins enable rabbitmq_web_mqtt

# 컨테이너 나가기
exit
```
1. **관리자 웹 인터페이스 접속 확인**

- 브라우저에서 `http://localhost:15672` 접속
- Username: admin
- Password: admin123

1. **컨테이너 관리 명령어**
``` bash
# 컨테이너 상태 확인
docker ps

# 컨테이너 중지
docker stop rabbitmq

# 컨테이너 시작
docker start rabbitmq

# 컨테이너 로그 확인
docker logs rabbitmq

# 컨테이너 삭제 (중지 후)
docker rm rabbitmq
```
1. **Docker Compose 설정** (권장)
``` yaml
# docker-compose.yml
version: '3.8'
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # Management UI
      - "1883:1883"   # MQTT
      - "15675:15675" # MQTT over WebSocket
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin123
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
      - rabbitmq_log:/var/log/rabbitmq
volumes:
  rabbitmq_data:
  rabbitmq_log:
```
Docker Compose 사용:
``` bash
# 컨테이너 실행
docker-compose up -d

# 컨테이너 중지
docker-compose down
```
1. **연결 테스트** application.yml 설정:
``` yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123
```
