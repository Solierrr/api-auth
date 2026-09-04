# Finalidade do repositório

O `api-auth` é o serviço de autenticação e identidade da Solaria, responsável por registrar e autenticar usuários, emitir e renovar tokens JWT de acesso, gerenciar sessões e refresh tokens, e expor as chaves públicas via JWKS para que os demais serviços da organização validem tokens de forma independente. Além do fluxo local (e-mail e senha), o serviço suporta login federado via Firebase, vínculo de conta federada a uma conta local já existente e segundo fator via TOTP. Após um registro bem-sucedido, o serviço publica o evento de usuário criado em uma fila de outbox sobre Redis Streams, consumida pelo `api-persistence` para provisionar o registro definitivo do usuário — desacoplando a criação da identidade (aqui) do restante dos dados de domínio do usuário (lá).

<p>

[![License](https://img.shields.io/github/license/Solierrr/api-auth)](https://github.com/Solierrr/api-auth/blob/main/LICENSE)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/Solierrr/api-auth)](https://github.com/Solierrr/api-auth/commits)
[![GitHub Issues](https://img.shields.io/github/issues/Solierrr/api-auth)](https://github.com/Solierrr/api-auth/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/Solierrr/api-auth)](https://github.com/Solierrr/api-auth/pulls)
[![GitHub Contributors](https://img.shields.io/github/contributors/Solierrr/api-auth)](https://github.com/Solierrr/api-auth/graphs/contributors)
[![Release](https://img.shields.io/github/v/release/Solierrr/api-auth)](https://github.com/Solierrr/api-auth/releases)

</p>

<div align="center">

<p>
  <a href="https://github.com/syvixor/skills-icons">
    <img src="https://skills.syvixor.com/api/icons?i=kotlin,springboot,spring,postgresql,redis,firebase" height="48" alt="Stack do api-auth">
  </a>
</p>

<p>

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)

</p>

</div>

## Aprofunde-se no Projeto!

- [ARCHITECTURE.md](./ARCHITECTURE.md), camadas do serviço, padrão de outbox e árvore real do repositório.
- [RUNNING.md](./RUNNING.md), como subir o `api-auth` localmente e impedimentos conhecidos.
- [DEPLOYMENT.md](https://github.com/Solierrr/.github/blob/main/.github/DEPLOYMENT.md), pipeline de deploy padrão da organização (`main`/`qa`, Docker Hub, ArgoCD).

## Contribuindo

- [CONTRIBUTING.md](https://github.com/Solierrr/.github/blob/main/.github/CONTRIBUTING.md), convenções de commit, branch e Pull Request.
- [CODE_OF_CONDUCT.md](https://github.com/Solierrr/.github/blob/main/.github/CODE_OF_CONDUCT.md), código de conduta do projeto.
- [SECURITY.md](https://github.com/Solierrr/.github/blob/main/.github/SECURITY.md), como reportar vulnerabilidades de segurança.
