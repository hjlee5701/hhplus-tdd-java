## 동시성 문제와 멀티 스레드

동시성 문제란 **한 번에 여러 개의 스레드가 공유 자원에 동시에 접근하여 발생하는 문제**입니다.

스레드는 Main memory에 적재되어 있는 데이터를 CPU 캐시로 읽고 쓸 수 있습니다.

예를 들어, 한 스레드가 Main memory에 있는 A 데이터를 읽으면, CPU 캐시로 이를 로드한 후 값을 변경하고, 이후 수정된 A 데이터를 다시 Main memory에 쓰게 됩니다.

싱글 스레드 환경에서는 자원(A)의 상태 변화에 대해 문제가 발생하지 않지만, 멀티스레드 환경에서는 **자원 A가 공유 자원이 되어** 여러 스레드가 동시에 이를 수정하거나 읽으려 할 경우 **동시성 문제**가 발생할 수 있습니다.

이러한 문제는 데이터의 **일관성**과 **가시성**에 영향을 미칩니다.

동시성 문제로 인해 발생할 수 있는 주요 문제는 다음과 같습니다:

1. **데드락 (Dead Lock)**: 두 개 이상의 스레드가 서로가 가진 자원을 기다리며 무한 대기 상태에 빠지는 문제.
2. **경쟁 상태 (Race Condition)** : 두 개 이상의 스레드가 공유 자원에 동시에 접근하여 예상하지 못한 순서로 작업을 수행하는 문제.
3. **기아 상태 (Starvation)**: 특정 스레드가 자원을 얻지 못하고 계속 대기하는 문제.

이러한 문제를 해결하기 위해서는 **동기화** 기법이 필요합니다. 주요 개념은 다음과 같습니다:

1. **가시성**
    
    : 한 스레드에서 변경한 값이 다른 스레드에서 즉시 보이는 상태를 보장하는 것.
    
    이를 보장하기 위해서는 `volatile` 키워드와 같은 동기화 기법을 사용할 수 있습니다.
    
2. **원자성**
    
    : 하나의 작업이 중간에 끊기지 않고 완료되는 특성.
    
    즉, 작업이 완전히 끝나기 전까지는 다른 스레드가 이를 중간 상태로 볼 수 없도록 보장해야 합니다. `synchronized` 키워드나 `Lock`을 사용하여 원자성을 보장할 수 있습니다.
    

이 두 개념을 보장하는 동기화 기법을 적절하게 사용하여 **동시성 문제**를 해결할 수 있습니다.

---

## 자바와 동시성

자바는 멀티 스레드 환경에서 동작하도록 설계되어 있으며, 여러 가지 동시성 문제를 해결할 수 있는 다양한 방법을 제공합니다.

### 1. volatile

`volatile`은 변수 앞에 사용하는 키워드로, 해당 변수를 Main Memory 에서 직접 관리하겠다는 의미로 사용됩니다.

기본적으로 자바의 멀티 스레드 환경에서는 각 스레드가 **CPU 캐시**에서 데이터를 읽고 쓰게 되는데, `volatile` 키워드를 사용하면 해당 변수를 메인 메모리에서 관리하고, 모든 스레드가 동일한 값을 공유하게 됩니다.

- **장점**
    - 각 스레드는 메인 메모리에 있는 동일한 변수를 바라보게 되므로 **가시성**이 보장됩니다. 즉, 하나의 스레드가 값을 변경하면 다른 스레드들이 즉시 변경된 값을 볼 수 있습니다.
    - `volatile`은 주로 하나의 스레드가 **읽기** 작업을 하고, 다른 스레드가 **쓰기** 작업을 하는 경우에 유용합니다. 예를 들어, 어떤 플래그를 읽는 스레드와 이 플래그를 변경하는 스레드가 있을 때 적합합니다.
- **단점**
    - **캐시를 사용하지 않음**으로 인해 속도 면에서 성능이 떨어질 수 있습니다. 각 스레드가 값을 메인 메모리에서 직접 읽고 쓰기 때문에 캐시를 사용하는 것보다 느립니다.
    - **경쟁 상태**가 발생할 수 있습니다. `volatile`은 **원자성**을 보장하지 않기 때문에, 두 개 이상의 스레드가 동일한 변수를 동시에 **쓰려고** 할 경우, 의도하지 않은 결과가 발생할 수 있습니다.
- **결론**
    - `volatile`은 **가시성**을 보장하지만, **원자성**이나 **동기화**는 보장하지 않습니다.
    - 따라서 **경쟁 상태**를 방지하거나 여러 스레드 간의 **원자성**이 중요한 작업을 처리할 때는 `synchronized`나 `Lock`을 사용하는 것이 적합합니다.

---

### 2. Atomic

`atomic`은 원자성을 보장하기 위한 키워드로, synchronized와 달리 `CAS`(Compared And Swap)알고리즘으로 작동합니다.

`atomic` 원자적으로 값을 변경할 수 있도록 해주는 개념으로 `java.util.concurrent.atomic` 패키지 내에 여러 가지 Atomic 클래스를 제공합니다.

`CAS`(Compared And Swap)알고리즘으로 작동하며, 값의 변경을 `Lock` 없이 원자적으로 처리할 수 있게 합니다. 

`CAS`의 동작은 다음과 같습니다:

1. Main memory 에 저장된 값이 **기존 값**과 **일치하는지** 확인합니다.
2. **일치**하면, 메모리 주소의 값을 **새 값**으로 변경합니다.
3. **일치하지 않으면**, 변경을 하지 않고 **현재 값을 반환**합니다.

이 과정이 **원자적**으로 이루어지므로, 다른 스레드가 중간에 개입하지 않고 안정적으로 값을 수정할 수 있습니다.

- **장점**
    - Lock**을 사용하지 않으므로** Lock 대기 또는 경합을 피할 수 있어 성능이 향상됩니다.
    - 다수의 스레드가 동시에 CAS를 사용할 수 있기 때문에 **동시성**이 높아집니다.
    - CAS는 **원자적** 연산이므로, 연산 중에 다른 스레드가 값을 변경하는 상황을 처리할 수 있습니다. 이로 인해 **데이터 일관성**을 보장할 수 있습니다.
- **단점**
    - **반복적인 시도**가 필요할 수 있습니다.
        
        CAS를 성공적으로 수행하려면, 먼저 값을 수정한 다른 스레드와 경쟁하게 될 수 있습니다. 이때 **값이 변경되었으면**, 해당 스레드는 **다시 시도**해야 합니다.
        
- **결론**
    - **CAS**는 **원자적**으로 동작하여 멀티스레드 환경에서 동시성 문제를 해결하고 성능을 최적화하는데 매우 유용하지만, **반복적인 재시도**가 필요할 수 있다는 점은 단점으로 작용할 수 있습니다.

---

### 3. synchronized

동기화는 가시성과 원자성을 보장하여 동시성 문제를 제어할 수 있는 방법 중 하나입니다. 스레드 간 공유 자원에 대해 동일한 상태를 유지하도록 하여 공유 자원의 일관성을 보장합니다. 

동일한 상태를 유지하기 위해 `synchronized` 는 **Monitor Lock** 을 활용합니다.

`synchronized` 를 메서드 또는 코드 블럭(`this`)에 선언한다면, 스레드가 해당 객체의 모니터가 제공하는 **Lock(=** **Monitor Lock)** 을 획득함으로서 임계 영역에 진입할 수 있도록 만듭니다.

Lock을 가진 스레드 하나만 해당 영역에 접근할 수 있으며, 나머지 스레드는 모두 BLOCKED 상태로 대기하게 됩니다.

- **장점**
    - 메서드앞에 사용하여 간단하게 동시성 문제를 해결할 수 있다는 장점이 있습니다.
    - Lock 을 통해 **경쟁 상태** 문제를 해결할 수 있습니다.
- **단점**
    - **기아 상태**가 발생할 수 있습니다.
        
        Lock 을 기다리는 스레드는 JVM의 스케줄링 정책에 따라 순차적으로 진입할 수 있으나, 스레드의  일정한 순서가 정해지지 않습니다.
        
        즉, Lock 획득에 공정성이 보장 되지 않으므로 특정 스레드가 Lock 을 얻지 못하는 기아 상태가 발생할 수 있습니다.
        
    - 과도한 사용은 **데드락**이 발생할 수 있습니다.
        
        서로 다른 Lock 을 획득한 스레드가 있을 때, 각각 상대방이 갖고 있는 Lock 을 획득하기 위해 대기하는 경우 데드락 문제가 발생합니다.
        
- **결론**
    - `synchronized` 는 간결한 사용으로 동시성 제어 수단으로 사용됩니다. 단순히 임계 영역 보호에 적합니다. 또한 단일 객체에 대한 접근이 보호되어야 하는 경우 역시 적합합니다.
    - 하지만 Lock 에 대한 정밀한 제어는 불가능합니다. 또한 기아 상태와 데드락을 유발할 수 있습니다.

---

### 4. Reentrantlock

`ReentrantLock`은 `java.util.concurrent.locks` 패키지의 `Lock` 인터페이스를 구현한 클래스입니다. 이 클래스는 `Lock` 인터페이스의 기본적인 동작을 따르며, `synchronized` 블록보다 더 유연한 Lock을 제공합니다.

Lock 인터페이스에는 아래와 같은 메서드가 있습니다.

1. `lock()` : Lock **획득** 또는 **대기**합니다.
2. `tryLock()` : Lock **획득 가능 여부(true/false)** 반환합니다.
3. `tryLock(time)` : Lock 을 즉시 획득할 수 있다면 true 를 반환하고, 그렇지 않다면 **지정한 시간동안** Lock 획득을 시도 및 획득 가능 여부를 반환합니다.
4. `unLock()` : Lock 을 해제합니다.
5. `lockInterruptibly()` : Lock 획득 대기 중 인터럽트 발생 시 획득을 **포기**하고 `InterruptedException`을 던집니다.

`ReentrantLock`만 제공하는 추가 메서드 역시 있습니다.

- `isFair()`은  공정(fair) 모드로 설정되었는지 여부를 확인합니다. `FairSync`는 대기 중인 스레드가 `queue`에서 앞에 있을 때 Lock을 먼저 얻을 수 있도록 보장하는 방식입니다.
- 공정성 여부는 생성자에서 설정 가능합니다.
    
    ```java
    public final boolean isFair() {
        return sync instanceof FairSync;
    }
    ```
    
- 그 외에 `getHoldCount()` 같이 현재 스레드가 얼마나 많은 횟수로 Lock을 획득했는지, 즉 재진입 횟수를 반환하는 기능을 지원합니다.
    
    ```java
    public int getHoldCount() {
        return sync.getHoldCount();
    }
    ```
    
- **장점**
    - Lock을 **세밀하게 제어**할 수 있어, 시간 제한, 인터럽트 처리, Lock 획득 가능 여부 등을 조정할 수 있습니다.
    - **공정성**을 지원하여, 대기 중인 스레드가 공평하게 Lock을 획득할 수 있습니다.
    - 같은 스레드가 Lock을 여러번 **재진입** 가능합니다.
    
- **단점**
    - **Lock 해제를 누락**하면 데드락이 발생할 수 있습니다.
    - Lock을 보다 세밀하게 제어하기 때문에, 성능 상 **오버헤드**가 있을 수 있습니다.
        - `FairSync`는 **공정한 Lock 획득**을 보장합니다. 즉, 대기 중인 스레드가 FIFO 방식으로 Lock을 얻도록 하여, 특정 스레드가 Lock을 계속 획득하는 상황을 방지합니다.
        - 이 방식은 성능에 영향을 줄 수 있습니다.
- **결론**
    - `ReentrantLock`은 **세밀한 Lock 제어**와 **공정성**을 지원하지만, Lock **해제 관리**에 신경 써야 하며, 성능에 민감한 경우에는 적절한 사용이 필요합니다.

---

### 4. ConcurrentHashMap

`ConcurrentHashMap` 은 `HashTable` 과 `HashMap` 의 장점이 결합된 Map 구현체입니다.

`HashTable` 과 `HashMap` 의 장점이 결합되어 있다는 특징이 있습니다.

- `HashTable` 은 모든 메서드에 `synchronized` 키워드가 적용되어 있어, **동시성은 보장**되지만 그만큼 **성능이 저하**됩니다.
- `HashMap` 은 멀티스레드 환경에서 **동시성 문제가 발생**할 수 있지만, **성능** 면에서는 뛰어난 장점이 있습니다.

즉, `ConcurrentHashMap`은 **동시성 문제**를 해결하면서도 **성능을 최적화**할 수 있는 자료 구조로 설계되었습니다.

주요 특징은 다음과 같습니다.

1. 독립적인 Lock 관리
    - 기본 16개의 버킷으로 데이터를 분할하며, 버킷마다 **Lock 을 독립적**으로 관리하고 있습니다.
2. 읽기 작업에 Lock 미사용
    - 쓰기 작업 (`put()`, `remove()` 등) 에는 `synchronized` 를 사용해서 동기화를 처리합니다.

- **장점**
    - 병렬 처리
        - 버킷마다 Lock 을 관리하고 있으므로, 성능이 향상됩니다. 예를 들어, 16개의 스레드가 서로 다른 버킷에 접근할 경우 모든 스레드는 병렬 처리가 가능합니다.
    - 읽기 작업
        - 읽기 작업에는 Lock 을 사용하지 않으므로, 동시성과 높은 성능을 보장합니다.
    - 동시성 최적
        - 서로 다른 스레드가 같은 버킷에 접근할 경우, 빈 버킷에는 synchronized 를 사용 안하는 등, Lock 의 범위를 최소화 합니다.
- **단점**
    - **많은 쓰기 작업**이 이루어지는 환경에서는 성능 저하가 발생할 수 있습니다. 특히 **동일한 버킷에 대해 여러 스레드가 동시에 쓰기를 시도**하는 경우 성능 저하가 발생할 수 있습니다.
    - `ConcurrentHashMap`은 **여러 버킷을 사용**하고 각 버킷에 대해 락을 독립적으로 관리하므로, `HashMap`에 비해 메모리 사용량이 더 많을 수 있습니다.

---

## 동시성 문제 해결

동기화를 제어하는 가장 간단한 방법은 `synchronized` 키워드의 사용입니다. 포인트 충전이나 사용 메서드에 해당 키워드를 붙이면 동시성 제어가 가능합니다.

그러나 `synchronized`는 다음과 같은 한계가 있습니다:

- Lock 획득/해제에 대한 세밀한 제어가 불가능하고,
- 공정성(fairness) 보장이 없으며,
- 데드락 위험이 존재합니다.

이러한 단점을 보완하기 위해 `ConcurrentHashMap`과 `ReentrantLock`을 함께 사용하면 더 나은 성능과 제어력을 확보할 수 있습니다.

예를 들어 포인트 충전 로직을 설명하자면 다음과 같습니다

1. `ConcurrentHashMap`은 내부적으로 여러 개의 버킷을 사용합니다.
    
    예를 들어 `userId_A`와 `userId_B`가 각기 다른 버킷에 속하면 두 개의 put() 연산은 병렬로 수행됩니다.
    
    반면 동일한 버킷에 속한 경우엔 내부 `synchronized` 블록에 의해 순차적으로 처리됩니다.
    
2. 포인트 충전 시, `userId` 단위로 분리된 `ReentrantLock`을 획득하게 됩니다.
    
    `userId_A`와 `userId_B`는 서로 다른 Lock을 사용하므로, 각자의 임계 구역에 병렬로 접근할 수 있습니다.
    
3. 하지만 동일한 `userId`에 대해 여러 요청이 동시에 들어오면, 동일한 Lock 인스턴스를 공유하게 됩니다.
    
    이 경우 먼저 Lock을 획득한 스레드만 임계 구역에 진입하고, 나머지는 대기하게 됩니다.
    

이 방식은 다음과 같은 장점을 가집니다

- `userId` 단위로 Lock 을 분리하여 병렬성을 높일 수 있고,
- 불필요한 전체 동기화를 피함으로써 성능을 향상시킬 수 있습니다.
