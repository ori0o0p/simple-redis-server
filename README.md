# Netty-based Redis Server
![image](https://github.com/user-attachments/assets/8b49a2f2-de6b-4e9a-ab9d-f5c475c8eab5)

## 목표

- 데이터 저장 : SET
- 데이터 조회 : GET

Key-Value 인메모리 스토어 역할을 할 수 있을 정도만 구현.

예를 들어 Redis의 INCR, DEL과 같은 고오급~ 명령어는 구현 대상에서 제외.

Redis Server만 구현 대상이기 때문에 Redis Client는 구현 대상에서 제외.

### Netty Redis Server 구성 요소

- Key-value 저장소 : In-memory Data Store
    - Java Collections Framework의 Map 자료구조를 사용 예정
    - Map 자료구조에서 Thread-safe하게 사용 가능한 ConcurrentMap 사용,
      구현체로 ConcurrentHashMap 클래스 사용
- 클라이언트 연결 수락을 하기 위한 Event Loop Group
     ![image](https://github.com/user-attachments/assets/e9a612e5-9d53-4192-9c8a-78cfd328dc92)

    - Netty Threading Model과 같이 클라이언트의 연결을 수락할 수 있는 Boss Group,
      연결된 클라이언트의 실제 데이터 통신(I/O처리)를 하는 Worker Group으로 구성
    - NIO API의 기능 활용하기 위해 NioEventLoopGroup 사용, Boss Group는 1개로 고정 NioEventLoopGroup(1)
- Netty Server 설정 초기화의 단순화를 위한 헬퍼 클래스. Boot Strap
    - ServerBootstrap을 사용하여 소켓 채널 바인딩, Event Loop Group 셋팅, 채널 초기화 pipeline 구성
- 새로운 연결을 수락할 소켓 채널
    - NIO API의 기능 활용하기 위해 NioServerSocketChannel 사용.
![image](https://github.com/user-attachments/assets/60b5ddd6-9cd1-419f-af83-2c4ee33704d0)

- 채널 초기화 pipeline
  ![image](https://github.com/user-attachments/assets/6150cbb9-2d68-4387-ae60-6467c90c03f2)
    <i><small>이해만을 돕기 위한 다이어그램</small></i>
    
    - ServerBootstrap을 사용하여 채널 초기화 pipeline를 구성, ChannelPipeline를 사용하여  
      인’아웃 바운드의 이벤트 흐름 제어
    - Handler
        - RedisDecoder, RedisBulkStringAggregator, RedisArrayAggregator 사용,
          Redis 프로토콜을 디코딩하고, 적절한 객체로 변환할 수 있도록 도와주는 핸들러들
    - RedisEncoder
        - Redis 응답을 인코딩하여 클라이언트로 전송 역할
    - RedisCommandHandler
        - 실제 Redis 명령(인바운드 메시지)을 처리
- 클라이언트의 인바운드 메세지 처리 로직
    - GET, SET 메세지 처리 로직 구현 - channelRead0 메서드,
      Redis 명령에 따른 적절한 명령 핸들러로 라우팅 (handleSetCommand, handleGetCommand)

### 데이터 저장, 조회 처리 과정

- GET : 데이터 조회
    - 입력된 명령어의 인자 개수가 2개인지 검증 → GET {key}, 2개가 아니라면 에러 발생
    - key 추출 후 해당 key로 dataStore에서 조회. 존재하지 않으면 NULL_INSTANCE 반환
- SET : 데이터 저장
    - 입력된 명령어의 인자 개수가 3개인지 검증 → SET {key} {value}, 3개가 아니라면 에러 발생
    - key, value 추출 후 dataStore에 저장
      데이터가 성공적으로 저장되면 클라이언트에게 OK 메시지 전송




<s>저는 레디스가 싫어요</s>
