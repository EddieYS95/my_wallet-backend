# MY WALLET

- Spring Boot, JPA, Web3J, PostegreSQL를 기본으로 사용했습니다.

### API 요구사항

#### 1. 지갑 생성

```http request
POST {{url}}/wallets

request body : {
    "password": "16자리의 숫자 + 영문 조합의 문자열"
}
```

- 사용자가 설정한 16자리의 비밀번호를 기반으로 Private Key 암호화 수행

#### 2. 지갑 ETH 잔액 조회

```http request
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

```http request
POST {{url}}/transactions

request body : {
    "fromAddress": 출금할 지갑의 주소,
    "password": 출금할 지갑의 패스워드 (지갑 생성시 입력한 암호),
    "toAddress": 입금할 지갑의 주소,
    "eth": 입급할 금액
}

response : {
    "Transaction Created" 출력시 송금 성공
}
```

#### 4. 입출금 이벤트 조회

```http request
GET {{url}}/wallets/:address/transactions

request param: {
    "address": 입출금 이벤트를 조회할 지갑의 주소
}

response : {
    "idfTransaction": 고유 식별 번호,
    "hash": Transaction의 Hash값,
    "status": 트랜잭션 처리상태 (PENDING, MINED, CONFIRMED),
    "blockConfirmation (Optional)": 트랜잭션이 블록에 채굴된 뒤 추가로 쌓인 블록의 수,
    "value": 전송할 ETH의 양,
    "fee (Optional)": 전송시 사용된 ETH 수수료,
    "from": 출금한 지갑의 주소,
    "to": 입금받은 지갑의 주소
}
```
