# User Management

发布日期：2025年8月4日

## User-domain

添加应用程序的第一件事是用户管理和安全性。

创建一个类 User 来保存用户的所有属性。现在，从简单开始，仅使用以下属性：

| property | description                                          |
| -------- | ---------------------------------------------------- |
| Name     | 用户的全名。                                         |
| Email    | 用户的电子邮件地址，也将用作登录的用户名。           |
| Password | 用户的密码。                                         |
| Role     | 用户在系统中的角色，定义用户可以做什么和不能做什么。 |

从以下用户代码开始：

```java
// User.java
package com.example.copsboot.users;

import java.util.Set;
import java.util.UUID;

public class User {
    private UUID id;
    private String email;
    private String password;
    private Set<UserRole> roles;

    public User(UUID id, String email, String password, Set<UserRole> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }
}
```

定义应用程序中用户的角色：

```java
// UserRole.java

 package com.example.copsboot.users;
 public enum UserRole {
    OFFICER,
    CAPTAIN,
    ADMIN
 }
```

 要使用 Spring Boot Data JPA 持久化您的第一个域类，您必须从 Java Persistence API （JPA） 规范中添加一些注释：

```java
@Entity
@Table(name = "copsboot_user")
public class User {
    @Id
    private UUID id;
    private String email;
    private String password;
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles;

 protected User() { 
    }

 public User(UUID id, String email, String password, Set<UserRole> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
```

① @Entity将类标记为 JPA 的持久实体。

② @Table标注是可选的。它允许您显式设置要用于数据库表的名称。

③ id 字段用 @Id 进行注释，以将其标记为实体的主键。

④ roles 字段是枚举值的集合。@Enumerated（EnumType.STRING） 确保枚举值存储为字符串值。

此应用程序使用“早期主键”生成。这意味着它不依赖于数据库来提供主键，而是首先创建一个主键，该主键传递到 User 对象的构造函数中。这样做的主要优点是你永远不会有“不完整”的对象，并且它更容易实现相等。

## User-repository

创建此接口，您可以在运行时拥有一个存储库，允许您保存、编辑、删除和查找用户实体。

```java
package com.example.copsboot.users;

import org.springframework.data.repository.CrudRepository;
import java.util.UUID;
 
public interface UserRepository extends CrudRepository<User, UUID> {
}
```

要检查一切是否正常，请为其创建一个测试：

```java
package com.example.copsboot.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    public void testStoreUser() {
        HashSet<UserRole> roles = new HashSet<>();
        roles.add(UserRole.OFFICER);
        User user = repository.save(new User(UUID.randomUUID(),
                "alex.foley@beverly-hills.com",
                "my-secret-pwd",
                roles));
        assertThat(user).isNotNull();
        assertThat(repository.count()).isEqualTo(1L);
    }
}
```

① 需要用 @ExtendWith(SpringExtension.class) 对 JUnit 测试类进行注释，以启用 Spring Boot 的测试支持。

② @DataJpaTest指示测试支持仅启动负责与JPA相关的所有内容的应用程序部分。

③  注入 UserRepository，以便在单元测试中使用。

④  这是包含您的测试的方法。

⑤ 将用户实体保存在此处的数据库中。

⑥ 仓库的save方法返回的对象应该返回一个非空对象。

⑦ 如果统计数据库中的 User 实体数量，则应该有一个。

在运行测试之前，您需要一个数据库。对于此类测试，获取测试的最简单方法是**将 H2 数据库放在类路径上**。如果我们这样做，Spring Boot 将创建一个实例并在测试中使用它。在pom.xml中添加新的依赖项：

```xml
<dependency>
	<groupId>com.h2database</groupId>
	<artifactId>h2</artifactId>
	<scope>runtime</scope>
</dependency>
```

 ① 依赖项已添加运行时范围（而不是测试范围），因为如果您使用 dev 配置文件运行应用程序，您也将使用 H2 启动应用程序本身。

运行测试应该成功。

现在，您可以将用户实体保存在数据库中。但是，**实施以下更改可以提高代码的可维护性**：

1. 使用专用主键类。
2. 为所有实体提取超类，以便以一致的方式定义主键。
3. 将主密钥生成集中在存储库中。

## Dedicated primary-key class

大多数示例使用 long 或 UUID 作为实体的主键。专用主键类具有以下优点：

- 它更清楚地表达了意图。如果变量的类型为 UserId，则很清楚您在说什么，而不是简单的 long 或 UUID。
- 无法将 UserId 值分配给 OrderId 或 BookId。这减少了在某处放置错误 ID 的机会。
- 如果您想将主键从 UUID 更改为 long，反之亦然，只需对应用程序代码进行最少的更改即可实现。

创建此 AbstractEntityId 类作为应用程序中所有 ID 类的基础：

```java
package com.copsboot.backend.entities;

import java.util.Objects;
import java.io.Serializable;
import jakarta.persistence.MappedSuperclass;
import com.google.common.base.MoreObjects;

@MappedSuperclass
public abstract class AbstractEntityId<T extends Serializable> implements Serializable, EntityId<T> {
    
    T id;

    protected AbstractEntityId() {

    }

    protected AbstractEntityId(T id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public T getId() {
        return id;
    }   

    @Override
    public String asString() {
        return id.toString();
    } 

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (this == o) {
            result = true;
        } else if (o instanceof AbstractEntityId) {
            AbstractEntityId other = (AbstractEntityId) o;
            result = Objects.equals(id, other.id);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .toString();
    }

}
```

接下来，定义 AbstractEntity。这将是实体的基类，并确保它们将使用您的 EntityId：

```java
package com.copsboot.backend.entities;

import java.util.Objects;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import com.google.common.base.MoreObjects;


@MappedSuperclass
public abstract class AbstractEntity<T extends EntityId> implements Entity<T> {
    
    @EmbeddedId
    private T id;

    protected AbstractEntity() {

    }

    protected AbstractEntity(T id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public T getId() {
        return id;
    }   

    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (this == o) {
            result = true;
        } else if (o instanceof AbstractEntityId) {
            AbstractEntityId other = (AbstractEntityId) o;
            result = Objects.equals(id, other.id);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .toString();
    }

}
```

为了完整起见，下面是 EntityId 和 Entity 的接口实现：

```java
package com.copsboot.backend.entities;

import java.io.Serializable;

public interface EntityId<T> extends Serializable {
    T getId();
    String asString();

}
```

asString 方法将 ID 作为字符串表示形式返回，例如用于 URL。您没有使用 toString，因为它通常用于调试目的，而您需要将其用作应用程序逻辑的一部分。

```java
package com.copsboot.backend.entities;

public interface Entity<T extends EntityId> {
    T getId();
}
```

完成所有这些后，您现在可以重构您的 User 类。首先，创建一个 UserId：

```java
package com.copsboot.backend.users;

import com.copsboot.backend.entities.AbstractEntityId;
import java.util.UUID;

public class UserId extends AbstractEntityId<UUID> {
   
    protected UserId(){ 
        
    }

    public UserId (UUID id) {
        super(id);
    }
}
```

① Hibernate 需要受保护的 no-args 构造函数才能工作。

② 这是应用程序代码应使用的构造函数。

在 User 类本身中，您可以删除 id 字段及其 getter。构造函数只是调用 super。重构后是这样的：

```java
package com.copsboot.backend.users;

import java.util.Collections;
import java.util.Set;
import com.copsboot.backend.entities.AbstractEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "copsboot_user")
public class User extends AbstractEntity<UserId> {

    private String email;
    private String password;
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles;

    protected User() { 
    }
    
    public User(UserId id, String email, String password, Set<UserRole> roles) {
        super(id);
        this.email = email;
        this.password = password;
        this.roles = roles;
    }
```

## Centralize primary-key generation

在单元测试中，您通过调用 UUID.randomUUID( )手动创建了主键。这对于 UUID 来说可能没问题，但如果你想使用 long，则肯定不是。因此，请在 UserRepository 上添加一个方法，如果要创建实体，该方法将提供要使用的“下一个”ID。

由于 UserRepository 是一个接口，因此您需要做一些额外的工作才能实现这一点。要开始使用，您需要创建一个 UserRepositoryCustom 接口：

```java
public interface UserRepositoryCustom {
    UserId nextId();
 }
```

每次调用 nextId 方法时，它都会返回一个新的 UserId 实例。步骤 2 将此接口添加到 UserRepository 界面：

```java
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, UserId>, UserRepositoryCustom {

    Optional<User> findByEmailIgnoreCase(String email);
}
```

步骤 3 创建一个实现 UserRepositoryCustom 接口方法的 UserRepositoryImpl 类：

```java
package com.copsboot.backend.users;

import java.util.UUID;

public class UserRepositoryImpl implements UserRepositoryCustom {
    private final UniqueIdGenerator<UUID> generator;

    public UserRepositoryImpl(UniqueIdGenerator<UUID> generator) {
        this.generator = generator;
    }

    @Override
    public UserId nextId() {
        return new UserId(generator.getNextUniqueId());
    }
}
```

当应用程序运行时，Spring Data 会将您自己的 UserRepositoryImpl 代码与 Spring Data 的 CrudRepository 代码相结合，因此 UserRepositoryCustom 和 CrudRepository 中的方法在您注入 UserRepository 的任何地方都可用。唯一 UUID 的生成放在 UniqueIdGenerator 后面。

测试方法本身现在需要更改为使用存储库中的 nextId 方法：

```java
    @Test
    public void testStoreUser() {
        HashSet<UserRole> roles = new HashSet<>();
        roles.add(UserRole.OFFICER);
        User user = repository.save(new User(repository.nextId(),
                "alex.foley@beverly-hills.com",
                "my-secret-pwd",
                roles));
        assertThat(user).isNotNull();
        assertThat(repository.count()).isEqualTo(1L);
    }
```

您可以使用 @Component 注释 InMemoryUniqueIdGenerator，但有一种不同的方法，特别是对于单元测试。您可以在单元测试类中创建一个静态内部类，并用@TestConfiguration对其进行注释。然后，您可以使用@Bean带注释的方法来定义单元测试中应可用的单例。对于您的测试，它看起来像这样：

```java
@TestConfiguration
    static class TestConfig {
        @Bean
        public UniqueIdGenerator<UUID> generator() {
            return new InMemoryUniqueIdGenerator();
            
        }
    }
```

在此之后，您的单元测试将为绿色。最后一步，您还需要在应用程序中提供这样的 bean。为此，请将相同的 bean 声明添加到 CopsbootApplication.java：

```
		@Bean
        public UniqueIdGenerator<UUID> generator() {
            return new InMemoryUniqueIdGenerator();
            
        }
```

您已经了解了如何使用 Spring Data JPA 存储实体以及如何测试它。
