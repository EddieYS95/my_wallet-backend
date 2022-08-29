# MY WALLET

- Spring Boot, JPA, Web3J, PostegreSQL를 기본으로 사용했습니다.

### API 요구사항

#### 1. 지갑 생성

```
POST {{url}}/wallets

request body : {
    "password": "16자리의 숫자 + 영문 조합의 문자열"
}
```

- 사용자가 설정한 16자리의 비밀번호를 기반으로 Private Key 암호화 수행

#### 2. 지갑 ETH 잔액 조회

```
GET {{url}}/wallets/:address

request param : {
    "address": "지갑의 주소(Address)"
}

response : {
    "address": "요청한 지갑의 주소",
    "balance": "지갑의 ETH 잔액 [ex) 15.798031039989080000]"
    "useableBalance": "실제 출금 가능한 ETH 잔액"
}
```

- Pending상태와 같은 트랜잭션의 영향으로 실제 출금이 가능한 잔액을 따로 표시합니다.

#### 3. 출금

```
POST {{url}}/transactions

request body : {
    "fromAddress": "출금할 지갑의 주소"
    "password": "출금할 지갑의 패스워드 (지갑 생성시 입력한 암호)"
    "toAddress": "입금할 지갑의 주소"
    "eth": "입급할 금액"
}

response : {
    "Transaction Created (출력시 송금 성공)" 
}
```

#### 4. 입출금 이벤트 조회

```
GET {{url}}/transactions/events

param: {
  size: 기본 10, 최대 100으로 한번에 조회할 이벤트의 갯수,
  page: 조회할 페이지 번호,
  starting_after: 조회를 시작할 시간 (yyyy-MM-ddTHH:mm:ss ex.2022-08-28T01:00:00)
  ending_before: 조회를 시작할 시간 (yyyy-MM-ddTHH:mm:ss ex.2022-08-28T01:00:00)
}

response : {
    "idfEvent": 이벤트 고유 식별 번호,
    "hash": 이벤트가 발생한 트랜잭션의 해시,
    "status": 이벤트가 발생한 시점의 트랜잭션 상태,
    "blockConfirmation": 이벤트가 발생한 시점의 트랜잭션 blockConfirmation,
    "createdAt": 이벤트 발생 시간
}
```

#### 5. 지갑의 트랜잭션 조회

```
GET {{url}}/wallets/:address/transactions

request param: {
    "address": "입출금 이벤트를 조회할 지갑의 주소"
}

response : {
    "idfTransaction": "고유 식별 번호"
    "hash": "Transaction의 Hash값"
    "status": "트랜잭션 처리상태 (PENDING, MINED, CONFIRMED)"
    "blockConfirmation (Optional)": "트랜잭션이 블록에 채굴된 뒤 추가로 쌓인 블록의 수"
    "value": "전송할 ETH의 양"
    "fee (Optional)": "전송시 사용된 ETH 수수료"
    "from": "출금한 지갑의 주소"
    "to": "입금받은 지갑의 주소"
}
```

### PACKAGE 구조

```
   backend
   ├──api
   │   ├── transaction
   │   ├── wallet
   ├──domain
   │   ├── transaction
   │   ├── wallet
   ├──exception
   ├──infra
   │   ├── blockchain
   │   ├── ctypto
   ├──observer
```

#### 1. API

api패키지는 MVC패턴에 맞게 설계되어 있습니다. 실제 사용자가 호출하게 될 4가지의 API 명세가 구현되어 있으며 도메인을 기준으로 하위 패키지를 구성했습니다.   
도메인은 사용자의 지갑을 의미하는 wallet과 실제 입출금이 일어나고 내역을 조회 가능한 transaction으로 이루어져 있습니다.

#### 2. Domain

각 도메인을 구성하는 데이터 모델(Entity, Repository)를 포함하고 있습니다. JPA를 기반으로 생성되어 각 Entity객체와 PostgreSQL의 테이블이
대응됩니다.  
API와 마찬가지로 도메인을 기준으로 서브 패키지가 구성됩니다.

#### 3. Exception

API의 생명주기에서 Runtime Exception을 처리해주는 패키지입니다. 에러코드에 따라 500(Internal Server Error) 또는 404(Not Found
Error)를 클라이언트에 전송합니다.

```json
{
  "errorCode": "에러 코드 번호",
  "errorMsg": "에러 메세지",
  "payload": "에러에 따른 추가 정보"
}
```

#### 4. Infra

어플리케이션의 기능을 위한 외부 모듈 또는 환경과 연결하기 위한 매니저, 커넥터, 유틸등을 포함하는 패키지입니다. web3j를 직접 사용하고 Wrapping해주는
blockchain패키지와 암호화를 위한 AesUtil을 포함한 crypto패키지로 구성됩니다.

#### 5. Observer

테스트넷과 실시간으로 연결하여 입출금 트랜잭션을 구독하고 Service를 이용해 데이터 처리를 관리합니다. API 패키지의 Service를 이용하며 구독 실행 및 해당 정보에 대한
Controller역할을 수행합니다.

* subscribeBlock의 Flowable를 TransactionService에서 처리시 @Transactional처리에 문제를 발견하여 Observer에서
  infra.blockchain.Ethereum을 직접 사용하여 TransactionService로 전파합니다.

### 테스트 시나리오

테스트 시나리오는 보내주신 테스트 시나리오를 기반으로 작성했습니다.

1. 지갑생성 API 호출

- 생성에 성공한 address의 주소를 출력해 줍니다.
- Private Key는 암호화 되어 Password없이 복호화가 불가합니다.

```json
{
  "code": 0,
  "payload": {
    "address": "0xc045685d4f8dee3190849dfa28d8adf655fc6977",
    "balance": 0
  }
}
```

2. 생성한 지갑으로 전송

- 생성한 지갑으로 1ETH를 전송합니다.
- wallets/:address/transactions를 통해 불러온 트랜잭션 정보입니다.
- 이더스캔을 통해 확인한 트랜잭션 정보를 함께 첨부합니다.

```json
{
  "idfTransaction": 71,
  "hash": "0x90ee71e66de7dd02eb483a1c6eba67ae7cce2d066b17a276c2667057e61e06ac",
  "status": "CONFIRMED",
  "blockConfirmation": 12,
  "value": 1.000000000000000000,
  "fee": 0.000031500000147000,
  "from": "0xfda13da0fb90999f9544971bc2ff87de9c525288",
  "to": "0xc045685d4f8dee3190849dfa28d8adf655fc6977"
}
```

![img.png](.github/img.png)

3. 2ETH 외부 지갑에 전송

- 이더리움 부족에 의해 트랜잭션이 실패합니다.
- 500 (Internal Server Error)와 함께 지갑 부족에 대한 안내를 출력합니다.

```json
{
  "errorCode": 201,
  "errorMsg": "NotEnough_Balance",
  "payload": "지갑의 잔액이 부족합니다."
}
```

4. Transaction 수행 (0.5ETH 전송)

- 1ETH를 받은 지갑으로 0.5ETH 전송

```json
{
  "idfTransaction": 72,
  "hash": "0x76b88b2f1e290e344e8466295524d340d18bae2261a64a470b9e9b549fdf1c94",
  "status": "CONFIRMED",
  "blockConfirmation": 12,
  "value": 0.500000000000000000,
  "fee": 0.000450960000960000,
  "from": "0xc045685d4f8dee3190849dfa28d8adf655fc6977",
  "to": "0xFDA13da0FB90999F9544971bC2fF87DE9c525288"
}
```

5. 0.5 ETH 전송

- Confirmed 이전에 사용 가능한 잔액을 초과한 금액의 출금을 요청할 경우 별도의 에러로 처리했습니다.

```json
{
  "errorCode": 202,
  "errorMsg": "NotEnough_ChainBalance",
  "payload": "출금가능한 지갑의 잔액이 부족합니다. PENDING중이거나 MINED상태의 트랜잭션을 확인해 주세요."
}
```

6. EventList API 호출

- 시간의 역순으로 출력

```json
[
  {
    "idfEvent": 170,
    "hash": "0x76b88b2f1e290e344e8466295524d340d18bae2261a64a470b9e9b549fdf1c94",
    "status": "CONFIRMED",
    "blockConfirmation": 12,
    "createdAt": "2022-08-29 06:15:06"
  },
  {
    "idfEvent": 169,
    "hash": "0x76b88b2f1e290e344e8466295524d340d18bae2261a64a470b9e9b549fdf1c94",
    "status": "MINED",
    "blockConfirmation": 12,
    "createdAt": "2022-08-29 06:15:06"
  },
  ...
  {
    "idfEvent": 158,
    "hash": "0x76b88b2f1e290e344e8466295524d340d18bae2261a64a470b9e9b549fdf1c94",
    "status": "MINED",
    "blockConfirmation": 1,
    "createdAt": "2022-08-29 06:12:36"
  },
  {
    "idfEvent": 157,
    "hash": "0x76b88b2f1e290e344e8466295524d340d18bae2261a64a470b9e9b549fdf1c94",
    "status": "PENDING",
    "blockConfirmation": 0,
    "createdAt": "2022-08-29 06:12:12"
  },
  {
    "idfEvent": 156,
    "hash": "0x90ee71e66de7dd02eb483a1c6eba67ae7cce2d066b17a276c2667057e61e06ac",
    "status": "CONFIRMED",
    "blockConfirmation": 12,
    "createdAt": "2022-08-29 06:08:57"
  },
  {
    "idfEvent": 155,
    "hash": "0x90ee71e66de7dd02eb483a1c6eba67ae7cce2d066b17a276c2667057e61e06ac",
    "status": "MINED",
    "blockConfirmation": 12,
    "createdAt": "2022-08-29 06:08:57"
  },
  {
    "idfEvent": 154,
    "hash": "0x90ee71e66de7dd02eb483a1c6eba67ae7cce2d066b17a276c2667057e61e06ac",
    "status": "MINED",
    "blockConfirmation": 11,
    "createdAt": "2022-08-29 06:08:56"
  },
  ...
]
```

### 회고

블록체인기술을 완전히 처음 접했습니다. 간단한 지갑을 만들면서 정말 재밌었고 매력적이라는 생각을 했습니다. 또한 많은 기술적 고민들을 하면서 의아했던 부분 그리고 결정의 과정을
회고로 전달하고자 합니다. 개발을 수행하면서 최대한 안정적인 방법을 선택하려 노력했지만 정답이 아닐 수 있겠다고 생각했던 지점들이 너무 많아 시간이 꽤 소요 됐습니다.

물론 엔지니어라면 코드로 이야기해야 하지만 프로젝트를 진행하면서 너무 많은, 토론하고 싶었던 포인트들이 있어 이렇게 회고로라도 전달하고자 합니다.

#### 1. 노드 연결

블록체인 기술을 코인으로만 알고 있었고 정확히 내용을 모르고 있어 항상 P2P로 연결되고 탈중앙화를 지향한다는데 어떤 기술을 사용하는지 의아했습니다. 테스트넷 노드에 연결을
수행하면서 어느정도 궁금증이 해소됐습니다. 당연할 수 있지만 결국 특정 노드의 End Point를 가지고 있어야 하고 해당 노드를 통해 체인에 접속한다는 부분에서 아쉽지만 가장
현실적인 방법이라 생각했습니다. (처음 탈중앙화때 문에 어떠한 노드의 End Point를 알지 못해도 접속하는 방법이 있을거라는 기대가 있었습니다.)

노드에 연결하고 나서 WebRPC기술을 활용해 통신을 수행하는 부분에서도 좋은 기술적 선택였구만 느꼈습니다. 각 노드의 트랜잭션을 모두 함께 처리해야하고 완전한 체인 형태로
구성되려면 노드간 실시간 통신 및 정보 공유는 필수라 느꼈기 때문입니다.

#### 2. Flowable

Web3j를 이용해 노드에 접속하면서 Flowable을 활용해 실시간 블록 또는 트랜잭션의 이벤트를 처리했습니다. 스트림을 이용한 구독 / 소비 구조로 설계를 함께 진행했습니다.
여기서 저는 처음에 transactionFlowable과 pendingTransactionFlowable을 활용해 pending과 채굴이 완료된 블록을 별도록 처리하고자 했습니다.
여기서 pendingTransactionFlowable가 노드단에서 response에러가 발생하는 것을 확인하고 다른 방법이 있는지 확인했지만 찾지 못 했습니다. 여기서 궁금한
포인트들이 있었습니다.

1. 각 이벤트의 Rpc이벤트는 테스트 넷의 노드가 직접 구현/ 제공 해야 하는 가? 또 이를 위한 추상화가 이루어져 있는가?
2. 그럼 PendingTransaction을 가져올 수 있는 다른 방법은 없는가?

이후 서버를 재실행 했을 때 누락된 트랜잭션을 복원하기 위해 replayPastBlock을 이용하여 이벤트를 구독했습니다. 그러면서 고민했던 포인트는 replayPastBlock은
특정 지점부터 Latest블록까지 순차적으로 처리하게 되는데 transactionFlowable과 동시에 수행해야하는가? 였습니다. 이렇게 됐을때 정확한 데이터를 순차적으로
기록하지 못할 수도 있겠다는 생각을 했습니다. 현재 내 잔액과 전송 가능한 잔액의 차이를 사용자에게 설명할 길이 없겠다는 생각이였습니다. 물론 실제 서비스라 생각하면 무중단
서비스를 제공하고 장애발생시 빠르게 복원한다면 어느정도 해결되겠지만 과거부터 현재 이벤트와 현재부터 앞으로 발생할 이벤트를 동시에 구독하여 처리하는 근본적인 문제를 해결하고
싶었습니다. 그래서 replayPastBlock이벤트를 우선 모두 처리하고 transactionFlowable을 처리하자고 판단하면서 replayPastBlock이 실행 지점이
아닌 현재지점의 Block까지 전달해줘야 한다는 기본 전제를 설정했습니다. 하지만 역시나 replayPastBlock은 구독 시점의 마지막 블록까지만 제공했습니다. 누락되는
블록들이 존재해서는 안되기 때문에 다른 방법을 고민했습니다. 그 과정에서 replayPastAndFutureBlocksFlowable 함수를 발견했고 해당 이벤트를 활용하여 블록을
구독 트랜잭션 처리를 수행했습니다. 고민했던 문제들은 모두 해결했지만 역시나 해당 이벤트도 완전하게 만족스럽지 못했는데 서버가 오랫동안 종료돼있다면 최근 발생하는 트랜잭션을
처리할때까지 상당시간이 걸린다는 문제였습니다. 물론 서비스의 오픈 타이밍을 현재 블록까지 모두 처리한 시점으로 잡으면 되겠지만 과거 누락된 블록을 전부 한번에 가져오는 방법이
없는지 무척 궁금해졌습니다.

#### 3. QueryDSL

처음 프로젝트를 수행하면서 최대한 적은 기술을 사용하고자 했습니다. 실제 너무 많은 기술이 포함된다면 읽는 사람입장에서 고려해야할 관점이 많아지기 때문입니다. 그래서
QueryDSL없이 순수 JPA를 활용하려 했지만 Event API를 제작하면서 QueryDSL 도입했습니다.

입력 데이터의 유동성을 허용하고 동적으로 쿼리의 최적화를 위해 QueryDSL을 사용하였고 Dto를 이용한 최적화도 함께 수행했습니다. (도입 전 초기에 너무 많은 if문으로
고통이였지만 편안해졌습니다)

#### 4. GasPrice

가스라는 개념이 처음에 너무 생소하게 받아들여졌습니다. 수수료개념이라 생각하여 내가 얼마를 내면 되는 구나 생각했는데 가스금액과 갯수를 따로 지정해야한다는 것을 알기 전까지 해당
금액을 맞추기 위해 삽질을 좀 했습니다. 구글의 도움을 받아 여러 레퍼런스를 확인했을 때 노드에서 제공하는 GasPrice를 이용하는 방법과 직전 블록의 GasPrice를 활용하는
두 가지 방법이 있던데 보편적인 방법은 어떤건지 궁금했습니다. (Meta Mask는 본인들의 계산법이 있는 것 같았습니다. 또 트랜잭션 방법이 여러가지 인것을 확인했습니다.
Burn?)

지갑을 처음 만들면서 들었던 생각이 데이터를 담아 보낼 수 있는데 채팅마냥 너도 나도 마구 데이터를 전송하면 어쩌지 라는 생각을 했는데 해당 문제를 Gas Price라는 수수료로
해결했겠다는 생각도 같이한 것 같습니다.

#### 정리

사실 제게 해당 프로젝트는 매우 어렵게 느껴졌습니다. 블록체인을 기술적으로 처음 접했기 때문에 모든 코드에서 고민을 했고 정확하게 사용하는 건지에 대한 의문 때문에 진도가 빠르게
나가기 어려웠습니다. 분명히 더 좋은 방법이 있을 것 같다는 고민을 했던 것 같습니다.

또 이런 문제 때문에 보내주신 요구사항을 한 번에 파악하기 어려웠던 것 같습니다. 그래서 API를 한 두개 더 만들었는데 그 과정에서 해당 기술의 이해도가 어느정도 생기기도
했습니다. (삽질에 대한 합리화)

이런 많은 고민을 해서 그런지 구조에 대한 설계 부분 에서 놓친 부분이 많은 것 같아 자신있다는 느낌은 아닙니다. 그래도 데이터의 흐름과 역할을 나누어 설계했으며 각 기능에 대해
컴포넌트화를 하기 위해 노력했습니다.

> 회고가 너무 길어지는 것 같아 여기까지 줄이겠습니다.  
> 기회가 주어진 다면 더 깊은 토론을 하고 싶네요!