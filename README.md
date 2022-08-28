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