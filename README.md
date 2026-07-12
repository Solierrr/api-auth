# Roadmap de Implementação - API de Autenticação (Spring Boot + Kotlin)

## Objetivo

Implementar uma API responsável exclusivamente pela identidade e autenticação dos usuários do sistema Solaria.

### Responsabilidades

- Cadastro de empresas
- Cadastro de funcionários via chave de acesso
- Login
- Logout
- JWT
- Refresh Token
- Google OAuth2
- Microsoft OAuth2
- 2FA
- Recuperação de senha
- Gerenciamento de usuários
- Gerenciamento de cargos e permissões
- Gerenciamento de chaves de acesso

---

# Fase 1 - Inicialização

## Configuração do projeto

- [ ] Criar projeto Spring Boot (Kotlin)
- [ ] Configurar Gradle
- [ ] Configurar PostgreSQL
- [ ] Configurar Profiles
- [ ] Configurar OpenAPI
- [ ] Configurar Actuator
- [ ] Configurar Docker

### Dependências

- Spring Web
- Spring Security
- Spring Data JPA
- Validation
- OAuth2 Client
- PostgreSQL Driver
- JWT (jjwt)
- Springdoc OpenAPI
- Actuator

---

# Fase 2 - Estrutura do Projeto

Criar a arquitetura inicial.

```
src/main/kotlin/com/solaria/auth

config/
controller/
dto/
entity/
exception/
mapper/
repository/
security/
service/
validation/
util/
```

Dentro de validation:

```
validation
├── annotation
├── validator
├── regex
└── util
```

---

# Fase 3 - Modelagem do Banco

## Entidades

- [ ] Company
- [ ] User
- [ ] Person
- [ ] Position
- [ ] Permission
- [ ] PositionPermission
- [ ] AccessKey
- [ ] RefreshToken
- [ ] OAuthAccount
- [ ] TwoFactorSecret

---

## Relacionamentos

- Company possui vários Users
- User pertence a uma Company
- User possui uma Position
- Position possui várias Permissions
- AccessKey pertence a uma Company
- AccessKey concede uma Position

---

# Fase 4 - Repositories

Criar os repositories.

- [ ] CompanyRepository
- [ ] UserRepository
- [ ] PersonRepository
- [ ] PositionRepository
- [ ] PermissionRepository
- [ ] PositionPermissionRepository
- [ ] AccessKeyRepository
- [ ] RefreshTokenRepository

---

# Fase 5 - DTOs

## Auth

- [ ] LoginRequest
- [ ] LoginResponse
- [ ] RefreshRequest
- [ ] RefreshResponse

---

## Company

- [ ] RegisterCompanyRequest
- [ ] CompanyResponse

---

## User

- [ ] RegisterEmployeeRequest
- [ ] UserResponse
- [ ] UpdateUserRequest

---

## Access Key

- [ ] CreateAccessKeyRequest
- [ ] AccessKeyResponse

---

# Fase 6 - Validações

Criar validações customizadas.

## CPF

- [ ] @ValidCpf
- [ ] CpfValidator
- [ ] CpfUtils

---

## CNPJ

- [ ] @ValidCnpj
- [ ] CnpjValidator
- [ ] CnpjUtils

---

## Senha Forte

- [ ] @StrongPassword
- [ ] StrongPasswordValidator

---

## Chave de acesso

- [ ] @ValidAccessKey
- [ ] AccessKeyValidator

---

## Email único

- [ ] @UniqueEmail
- [ ] UniqueEmailValidator

---

## Regex

Centralizar todos os regex.

```
RegexPatterns

EMAIL

PASSWORD

PHONE

ACCESS_KEY
```

---

# Fase 7 - Exceptions

Criar tratamento global.

```
@RestControllerAdvice
```

Exceptions

- UserAlreadyExistsException
- InvalidCredentialsException
- InvalidTokenException
- AccessKeyExpiredException
- AccessKeyNotFoundException
- AccessDeniedException
- CompanyNotFoundException

---

# Fase 8 - CRUD Base

## Empresa

- [ ] Criar empresa
- [ ] Buscar empresa
- [ ] Atualizar empresa

---

## Usuário

- [ ] Buscar usuário
- [ ] Atualizar usuário
- [ ] Desativar usuário

---

# Fase 9 - Chaves de Acesso

Implementar fluxo completo.

## Empresa

```
POST /access-keys
```

Gerar

- código
- cargo
- validade
- limite de usos

---

## Funcionário

```
POST /auth/register
```

Fluxo

```
Recebe chave

↓

Busca chave

↓

Verifica validade

↓

Obtém empresa

↓

Obtém cargo

↓

Cria usuário

↓

Cria pessoa

↓

Incrementa usedCount
```

---

# Fase 10 - Spring Security

Configurar.

- [ ] PasswordEncoder
- [ ] SecurityFilterChain
- [ ] AuthenticationManager
- [ ] UserDetailsService
- [ ] AuthenticationProvider

---

# Fase 11 - JWT

Implementar.

Serviços

- JwtService
- JwtFilter
- JwtProperties

Endpoint

```
POST /auth/login
```

Fluxo

```
Email

↓

Senha

↓

BCrypt

↓

JWT

↓

Refresh Token

↓

Resposta
```

---

# Fase 12 - Refresh Token

Criar.

```
POST /auth/refresh
```

Fluxo

```
Refresh

↓

Validação

↓

Novo JWT

↓

Novo Refresh
```

---

# Fase 13 - Roles e Permissões

Integrar Position com Spring Security.

Utilizar

```
@PreAuthorize
```

Exemplos

- ADMIN
- MANAGER
- TECHNICIAN
- EMPLOYEE

---

# Fase 14 - Google OAuth2

Implementar login Google.

Fluxo

```
Google

↓

OAuth2

↓

Cria usuário (caso necessário)

↓

JWT próprio
```

---

# Fase 15 - Microsoft OAuth2

Mesmo fluxo do Google.

---

# Fase 16 - 2FA

Implementar TOTP.

Endpoints

```
POST /2fa/setup

POST /2fa/verify

DELETE /2fa
```

Fluxo

```
Login

↓

Senha

↓

Código TOTP

↓

JWT
```

---

# Fase 17 - Recuperação de Senha

Endpoints

```
POST /password/forgot

POST /password/reset
```

---

# Fase 18 - Gerenciamento de Usuários

Empresa poderá

- listar usuários
- alterar cargo
- bloquear usuário
- desbloquear usuário
- excluir usuário

---

# Fase 19 - Auditoria

Registrar

- Login
- Logout
- Troca de senha
- Geração de chave
- Cadastro
- Tentativas inválidas
- Alterações de cargo

---

# Fase 20 - Testes

## Unitários

- Services
- Validators
- JWT
- Utils

---

## Integração

- Login
- Cadastro
- Refresh
- OAuth
- 2FA

---

# Fase 21 - Documentação

Documentar

- Endpoints
- DTOs
- Responses
- Códigos HTTP
- Fluxos de autenticação

---

# Fase 22 - Docker

Criar

- Dockerfile
- docker-compose

---

# Fase 23 - Kubernetes

Criar

- Deployment
- Service
- ConfigMap
- Secret
- Ingress

---

# Fluxo Geral

```
Empresa

↓

Cadastro

↓

Company

↓

Owner

↓

Login

↓

JWT

↓

Empresa gera AccessKey

↓

Funcionário recebe AccessKey

↓

Cadastro

↓

User + Person

↓

Login

↓

JWT

↓

Acesso às APIs
```

---

# Backlog Futuro

- Login com GitHub
- Login com Apple
- MFA via Email
- MFA via SMS
- Revogação de Tokens
- Lista negra de JWT
- Sessões simultâneas
- Rate Limiting
- Captcha
- Bloqueio por tentativas
- Auditoria avançada
- SSO (OpenID Connect)
- RBAC avançado
- ABAC (Attribute Based Access Control)