# account-manager

## Overview

A simple account manager that supports basic money operations (account creation and retrieval, deposits, withdrawals, transfers).

## Installation

* Build with `mvn clean install`.
* Run with `mvn exec:java -Dexec.mainClass="com.revolut.Main"`.

## API

|Operation|Description|
|-|-|
|`openAccount`|Opens and returns an empty account for the specified `ownerId`|
|`getAccount`|Returns the account for a specified `accountId`|
|`deposit`|Deposits the specified amount of `money` into `accountId`|
|`withdraw`|Withdraws the specified amount of `money` from `accountId`|
|`transfer`|Transfers specified amount of `money` from `sourceAccountId` to `targetAccountId`|

## Further Improvements

* Logging (deposits, withdrawals, tranfers, errors)
* Thread-safety
* Limits on the number of accounts per user
* Better documentation and testing

## Dependencies

#### Build

```
Java version "1.8.0_144"
Apache Maven 3.5.0
```

#### Project

* Jersey RESTful API framework
* Grizzly Server
* Google Guava
* JUnit

See [pom.xml](pom.xml)
