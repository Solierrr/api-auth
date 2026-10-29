# Arquitetura do Repositório

O `api-auth` segue uma arquitetura em camadas típica de um serviço Spring Boot (`controller` → `service`/`service.impl` → `repository` → `domain`), com dois eixos adicionais que não aparecem em um CRUD comum: um módulo `security` dedicado a emissão/validação de JWT e integração com Firebase, e um módulo `outbox` que implementa o padrão Transactional Outbox para publicar eventos de domínio sem acoplar o commit da transação de negócio à disponibilidade do Redis. A comunicação com outros serviços da organização (hoje, o `api-persistence`) é feita via HTTP autenticado por token de serviço M2M, isolada no pacote `integration`.

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,springboot,postgresql,redis" height="48" alt="Arquitetura do api-auth">
  </a>
</p>

- **Autenticação híbrida**, login local por e-mail/senha (`AuthService`/`AuthServiceImpl`) e login federado via Firebase (`FirebaseAuthenticationService`/`FirebaseAuthenticationServiceImpl`), com endpoint dedicado para vincular uma conta federada a uma conta local já existente (`/auth/firebase/link`).
- **Tokens JWT assinados com par de chave RSA**, carregados de um keystore PKCS12 (`JwtKeysConfig`, `app.security.jwt.keystore-path`), com a chave pública exposta em `/.well-known/jwks.json` (`JwksController`) para que outros serviços validem tokens sem chamar o `api-auth` a cada request.
- **Refresh token rotativo e sessão**, `SessionService`/`RefreshTokenService` controlam o ciclo de vida da sessão e a rotação do refresh token a cada uso, reduzindo a janela de reuso em caso de vazamento.
- **Segundo fator (TOTP)**, entidade `TotpFactor` e `TwoFactorService` implementam autenticação em dois fatores baseada em tempo (RFC 6238), independente do provedor de login usado.
- **Outbox assíncrono sobre Redis Streams**, `OutboxPollingScheduler` lê a tabela `outbox_event` (Postgres) e publica na stream configurada (`OutboxStreamPublisher`); `OutboxDeadLetterReclaimer` reclama mensagens não confirmadas (visibility timeout) e move entregas que excederam `max-delivery-attempts` para uma stream de dead-letter. A stream e a dead-letter são aparadas no `XADD` por `MAXLEN` aproximado (`OUTBOX_STREAM_MAXLEN`, padrão 10000). O agendamento desses jobs é ligado explicitamente em `SchedulingConfig` (`@EnableScheduling`), já que a aplicação não tem agendamento habilitado por padrão.
- **Integração service-to-service com `api-core`**, `CoreUserClient` provisiona o usuário definitivo em `POST /internal/users`, autenticado pelo cabeçalho `X-Service-Key` com a chave deste serviço (`SERVICE_KEY`) — o `api-auth` é a fonte da identidade/credencial, o `api-core` é a fonte dos dados de domínio do usuário.
- **Tokens do Google Calendar por técnico**, `GoogleCalendarTokenController` expõe `GET`, `PUT` e `DELETE` em `/internal/technicians/{technicianId}/google-calendar-token` e `PATCH .../revoke` para o `google-registry`, numa `SecurityFilterChain` própria que exige o cabeçalho `X-Service-Key` (`SERVICE_KEY_GOOGLE_REGISTRY`). Os tokens ficam na tabela `google_calendar_token`, chaveada por `technician_id` e criptografados com AES-256-GCM (`CALENDAR_TOKEN_ENCRYPTION_KEY`, 32 bytes em base64); fora de `CONNECTED` os segredos são apagados e o `GET` responde `404`.
- **Persistência via JPA/Hibernate + Flyway**, schema versionado em `src/main/resources/db/migration` (`ddl-auto=validate`, nunca `update`), banco Postgres compartilhado com o `api-core` conforme documentado em `.env.example`. O `V1__baseline.sql` é o único dono do DDL do `authdb`, com constraints e índices nomeados (`pk_`, `fk_`, `uq_`, `ck_`, `idx_`). O schema `export` expõe ao pipeline de dados só as views `auth_user`, `federated_identity` e `federated_provider_link`, sem credenciais, tokens, MFA, sessões nem eventos de segurança.
- **Coleção de testes de API em `http/`**, coleção [Bruno](https://www.usebruno.com/) com requisições locais organizadas por recurso, fora do escopo de testes automatizados (`src/test`).

```Tree do Repositório
├── .mvn/
│   └── wrapper/
├── .sonar/
├── http/
│   ├── autenticacao/
│   ├── chaves-publicas/
│   ├── firebase/
│   ├── environments/
│   ├── bruno.json
│   └── README.md
├── src/
│   ├── main/
│   │   ├── kotlin/com/solaria/auth/
│   │   │   ├── AuthApplication.kt
│   │   │   ├── config/
│   │   │   │   └── SchedulingConfig.kt
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.kt
│   │   │   │   ├── AuthExceptionHandler.kt
│   │   │   │   └── JwksController.kt
│   │   │   ├── domain/
│   │   │   │   ├── entity/
│   │   │   │   └── enums/
│   │   │   ├── dto/
│   │   │   │   ├── auth/{request,response}/
│   │   │   │   └── persistence/{request,response}/
│   │   │   ├── integration/persistence/
│   │   │   ├── outbox/
│   │   │   │   └── consumer/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   │   ├── config/
│   │   │   │   └── firebase/
│   │   │   └── service/
│   │   │       └── impl/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           └── V1__baseline.sql
│   └── test/
│       └── kotlin/com/solaria/auth/
├── .dockerignore
├── .editorconfig
├── .env.example
├── Dockerfile
├── LICENSE
├── README.md
├── ARCHITECTURE.md
├── RUNNING.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── sonar-project.properties
```
