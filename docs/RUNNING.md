# Rodando o Projeto Localmente

Este repositório é Kotlin + Spring Boot, buildado com Maven. O processo local é sempre o mesmo: clonar, abrir na IDE, baixar as dependências via `mvnw` e subir a aplicação. O `api-auth` depende de Postgres e Redis já em pé e, opcionalmente, de credenciais do Firebase — verifique a seção de impedimentos abaixo antes de iniciar.

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,springboot,spring,github,postgresql,redis" height="48" alt="Rodando o Projeto — api-auth">
  </a>
</p>

## Possíveis Impedimentos

- **JDK 21 instalado localmente**, a mesma versão usada no `Dockerfile` do repositório (`eclipse-temurin:21`) — rodar fora do container exige essa versão instalada e configurada como `JAVA_HOME`.
- **Postgres acessível**, o serviço usa `DB_POSTGRES_HOST`/`DB_POSTGRES_PORT`/`DB_POSTGRES_USER`/`DB_POSTGRES_PASSWORD` contra o banco `authdb` (`DB_POSTGRES_AUTH`), o mesmo host de Postgres compartilhado com o `api-core` — sem ele, o Flyway falha ao migrar o schema no boot.
- **Redis acessível**, `UPSTASH_CORE_HOST`/`UPSTASH_CORE_PORT` apontam para a mesma instância Upstash usada pelo `api-core`, necessária para a fila de outbox (`OutboxPollingScheduler`, `OutboxDeadLetterReclaimer` e o consumer). Para rodar sem Redis real, defina `OUTBOX_CONSUMER_ENABLED=false` — o polling/publish para a stream ainda depende de uma conexão Redis válida.
- **Par de chaves RSA (keystore PKCS12)**, `JWT_KEYSTORE_PATH`, `JWT_KEYSTORE_PASSWORD` e `JWT_ACTIVE_KID` precisam apontar para um keystore local válido — sem ele, a aplicação falha ao subir o `JwtKeysConfig` (assinatura dos tokens e endpoint `/.well-known/jwks.json`). {a confirmar: processo/script para gerar o keystore de desenvolvimento local}
- **Segredo de integração com `api-persistence`**, `SERVICE_CLIENT_SECRET` e `PERSISTENCE_BASE_URL` (padrão `http://localhost:8080`) são necessários para o provisionamento de usuário via `PersistenceServiceTokenClient`/`PersistenceUserClient` — sem o `api-persistence` no ar, o fluxo de registro completo não funciona ponta a ponta.
- **Credenciais do Firebase (opcional)**, o login federado só é ativado com `FIREBASE_ENABLED=true` e `FIREBASE_PROJECT_ID` configurado; com `FIREBASE_ENABLED=false` (padrão), os endpoints `/auth/firebase` e `/auth/firebase/link` ficam indisponíveis, mas o login local continua funcionando normalmente.
- **Secrets locais**, variáveis de ambiente equivalentes às injetadas em runtime pelo [Infisical](https://infisical.com) precisam ser criadas manualmente em `.env` na raiz do projeto (ver `.env.example` para a lista completa) — sem elas, a aplicação sobe mas falha ao tentar se conectar em dependências externas.

## Instalação do Projeto

### Iniciando o repositório com o Github

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=github,intellij" height="48" alt="Frameworks">
  </a>
</p>

Clone o repositório e abra no IntelliJ IDEA — o plugin Kotlin já vem embutido, sem configuração adicional.

```Comandos para clonar o repositório
git clone https://github.com/Solierrr/api-auth.git
cd ./api-auth
idea .
```

### Instalando dependências necessárias para rodar o projeto localmente

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=maven,apache" height="48" alt="Frameworks">
  </a>
</p>

Use sempre o wrapper (`mvnw`/`mvnw.cmd`) em vez de um Maven instalado globalmente, para garantir a mesma versão usada no CI. Antes de rodar, copie `.env.example` para `.env` e preencha as variáveis descritas na seção de impedimentos.

```Comandos para instalação de dependências
./mvnw dependency:go-offline
./mvnw spring-boot:run
```

Por padrão a aplicação sobe em `SERVER_PORT` (padrão `8081`, ver `application.properties`). Com o serviço no ar, o endpoint `GET /.well-known/jwks.json` é um bom smoke test — não depende de banco além do boot da aplicação e confirma que o keystore JWT foi carregado corretamente.

### Testando os endpoints

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=bruno" height="48" alt="Coleção de testes de API">
  </a>
</p>

O repositório inclui uma coleção [Bruno](https://www.usebruno.com/) em `TEST - Auth/` com requisições de exemplo para os endpoints de `/auth` (registro, login, login federado, refresh e logout). Abra a pasta diretamente no Bruno para importar a coleção.
